#include "game/include/launch.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include <windows.h>
#include <shlobj.h>
#include <string>
#include <vector>
#include <functional>

namespace ravex::game {

namespace {

std::wstring gameDir() {
    PWSTR known = nullptr;
    std::wstring profile;
    if (SHGetKnownFolderPath(FOLDERID_Profile, KF_FLAG_DEFAULT, nullptr, &known) == S_OK) {
        profile = known;
        CoTaskMemFree(known);
    }
    return joinPath(profile, L".minecraft");
}

std::wstring versionDir(const std::string& version, const std::string& loader) {
    std::wstring ver = fromUtf8(version);
    if (loader == "fabric") ver += L"-fabric";
    else if (loader == "forge") ver += L"-forge";
    else if (loader == "quilt") ver += L"-quilt";
    return joinPath(joinPath(gameDir(), L"versions"), ver);
}

std::wstring clientJarPath(const std::string& version, const std::string& loader) {
    std::wstring ver = fromUtf8(version);
    return joinPath(versionDir(version, ""), ver + L".jar");
}

void collectLibrariesRecursive(const std::wstring& dir, std::wstring& classpath) {
    std::wstring pattern = joinPath(dir, L"*");
    WIN32_FIND_DATAW fd;
    HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
    if (hFind == INVALID_HANDLE_VALUE) return;
    do {
        std::wstring name = fd.cFileName;
        if (name == L"." || name == L"..") continue;
        std::wstring child = joinPath(dir, name);
        if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
            collectLibrariesRecursive(child, classpath);
        } else if (name.size() > 4 && name.substr(name.size() - 4) == L".jar") {
            classpath += L";" + child;
        }
    } while (FindNextFileW(hFind, &fd));
    FindClose(hFind);
}

struct PipeReaderCtx {
    HANDLE hPipe;
    std::function<void(const std::string&)> callback;
};

DWORD WINAPI pipeReaderThread(LPVOID param) {
    auto* ctx = static_cast<PipeReaderCtx*>(param);
    char buf[4096];
    std::string lineBuf;
    DWORD bytesRead;
    while (ReadFile(ctx->hPipe, buf, sizeof(buf), &bytesRead, nullptr) && bytesRead > 0) {
        for (DWORD i = 0; i < bytesRead; ++i) {
            if (buf[i] == '\n') {
                if (!lineBuf.empty()) ctx->callback(lineBuf);
                lineBuf.clear();
            } else if (buf[i] != '\r') {
                lineBuf += buf[i];
            }
        }
    }
    if (!lineBuf.empty()) ctx->callback(lineBuf);
    delete ctx;
    return 0;
}

}

struct GameProcessImpl {
    HANDLE hProcess = nullptr;
    HANDLE hThread = nullptr;
    HANDLE hStdOutRead = nullptr;
    HANDLE hReadThread = nullptr;
    HANDLE hJob = nullptr;
    DWORD processId = 0;
};

bool GameProcess::valid() const { return impl != nullptr && impl->hProcess != nullptr; }

bool GameProcess::isRunning() const {
    if (!impl || !impl->hProcess) return false;
    DWORD exitCode = 0;
    if (!GetExitCodeProcess(impl->hProcess, &exitCode)) return false;
    return exitCode == STILL_ACTIVE;
}

DWORD GameProcess::getExitCode() const {
    if (!impl || !impl->hProcess) return 0;
    DWORD code = 0;
    if (GetExitCodeProcess(impl->hProcess, &code)) return code;
    return 0;
}

void GameProcess::kill() {
    if (impl && impl->hProcess) {
        TerminateProcess(impl->hProcess, 1);
    }
    if (impl && impl->hJob) {
        TerminateJobObject(impl->hJob, 1);
    }
}

void GameProcess::close() {
    if (impl) {
        if (impl->hStdOutRead) { CloseHandle(impl->hStdOutRead); impl->hStdOutRead = nullptr; }
        if (impl->hReadThread) { WaitForSingleObject(impl->hReadThread, 2000); CloseHandle(impl->hReadThread); impl->hReadThread = nullptr; }
        if (impl->hThread) { CloseHandle(impl->hThread); impl->hThread = nullptr; }
        if (impl->hProcess) { CloseHandle(impl->hProcess); impl->hProcess = nullptr; }
        if (impl->hJob) { CloseHandle(impl->hJob); impl->hJob = nullptr; }
        delete impl;
        impl = nullptr;
    }
}

int requiredJavaVersion(const std::string& mcVersion) {
    if (mcVersion.empty()) return 21;
    int major = 0;
    std::size_t dot = mcVersion.find('.');
    if (dot != std::string::npos) {
        major = std::stoi(mcVersion.substr(0, dot));
    } else {
        major = std::stoi(mcVersion);
    }
    int minor = 0;
    std::size_t dot2 = mcVersion.find('.', dot + 1);
    if (dot2 != std::string::npos) {
        minor = std::stoi(mcVersion.substr(dot + 1, dot2 - dot - 1));
    }
    if (major >= 1 && minor >= 21) return 21;
    if (major >= 1 && minor >= 17) return 17;
    if (major == 1 && minor >= 12) return 8;
    if (major == 0) return 8;
    return 17;
}

bool launchMinecraft(const LaunchParams& params, std::string* error,
                     const std::function<void(const std::string&)>& consoleLine,
                     GameProcess& outProcess) {
    if (params.mcVersion.empty()) {
        *error = "No version specified";
        return false;
    }
    std::wstring jar = clientJarPath(params.mcVersion, params.loader);
    if (!fileExists(jar)) {
        *error = "Client jar not found: " + toUtf8(jar);
        return false;
    }
    std::wstring vDir = versionDir(params.mcVersion, params.loader);
    std::wstring natives = ravex::nativesDir();
    std::wstring gDir = params.gameDir.empty() ? gameDir() : params.gameDir;
    std::wstring mcGameDir = gameDir();

    std::wstring classpath = jar;
    collectLibrariesRecursive(joinPath(mcGameDir, L"libraries"), classpath);

    std::wstring java = params.javaExe.empty() ? ravex::javaPath() : params.javaExe;
    std::wstring cmd = L"\"" + java + L"\"";
    if (!params.jvmArgs.empty()) cmd += L" " + fromUtf8(params.jvmArgs);
    cmd += L" -Xmx" + std::to_wstring(params.ramMb) + L"M";
    cmd += L" -Djava.library.path=\"" + natives + L"\"";
    cmd += L" -cp \"" + classpath + L"\"";
    if (params.loader == "fabric") {
        cmd += L" net.fabricmc.loader.impl.launch.knot.KnotClient";
    } else if (params.loader == "forge") {
        cmd += L" net.minecraftforge.fml.relauncher.ServerLaunchWrapper";
    } else if (params.loader == "quilt") {
        cmd += L" org.quiltmc.loader.impl.launch.knot.KnotClient";
    } else {
        cmd += L" net.minecraft.client.main.Main";
    }
    cmd += L" --username \"" + fromUtf8(params.username) + L"\"";
    cmd += L" --version \"" + fromUtf8(params.mcVersion) + L"\"";
    cmd += L" --gameDir \"" + gDir + L"\"";
    cmd += L" --assetsDir \"" + joinPath(mcGameDir, L"assets") + L"\"";
    cmd += L" --uuid \"" + fromUtf8(params.uuid) + L"\"";
    cmd += L" --accessToken \"" + fromUtf8(params.accessToken) + L"\"";
    cmd += L" --userType mojang";
    cmd += L" --versionType release";
    if (!params.assetIndexId.empty()) cmd += L" --assetIndex \"" + fromUtf8(params.assetIndexId) + L"\"";
    if (params.offline) cmd += L" --offline";

    SECURITY_ATTRIBUTES sa{};
    sa.nLength = sizeof(sa);
    sa.bInheritHandle = TRUE;
    sa.lpSecurityDescriptor = nullptr;

    HANDLE hJob = CreateJobObjectW(nullptr, nullptr);
    if (hJob) {
        JOBOBJECT_EXTENDED_LIMIT_INFORMATION jeli{};
        jeli.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE;
        SetInformationJobObject(hJob, JobObjectExtendedLimitInformation, &jeli, sizeof(jeli));
    }

    HANDLE hReadPipe = nullptr;
    HANDLE hWritePipe = nullptr;
    if (!CreatePipe(&hReadPipe, &hWritePipe, &sa, 0)) {
        *error = "Failed to create pipes";
        return false;
    }
    SetHandleInformation(hReadPipe, HANDLE_FLAG_INHERIT, 0);

    STARTUPINFOW si{};
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESTDHANDLES;
    si.hStdOutput = hWritePipe;
    si.hStdError = hWritePipe;
    si.hStdInput = GetStdHandle(STD_INPUT_HANDLE);

    PROCESS_INFORMATION pi{};
    std::vector<wchar_t> cmdBuf(cmd.begin(), cmd.end());
    cmdBuf.push_back(L'\0');

    BOOL ok = CreateProcessW(nullptr, cmdBuf.data(), nullptr, nullptr, TRUE,
                             CREATE_NO_WINDOW | CREATE_NEW_PROCESS_GROUP,
                             nullptr, vDir.c_str(), &si, &pi);
    CloseHandle(hWritePipe);

    if (!ok) {
        CloseHandle(hReadPipe);
        if (hJob) CloseHandle(hJob);
        *error = "Failed to start Minecraft (error " + std::to_string(GetLastError()) + ")";
        return false;
    }

    outProcess.impl = new GameProcessImpl();
    outProcess.impl->hProcess = pi.hProcess;
    outProcess.impl->hThread = pi.hThread;
    outProcess.impl->hStdOutRead = hReadPipe;
    outProcess.impl->hJob = hJob;
    outProcess.impl->processId = pi.dwProcessId;

    if (hJob) {
        AssignProcessToJobObject(hJob, pi.hProcess);
    }

    auto* readerCtx = new PipeReaderCtx();
    readerCtx->hPipe = hReadPipe;
    readerCtx->callback = consoleLine;
    outProcess.impl->hReadThread = CreateThread(nullptr, 0, pipeReaderThread, readerCtx, 0, nullptr);

    return true;
}

}
