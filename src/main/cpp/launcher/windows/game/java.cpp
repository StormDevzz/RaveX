#include "game/include/java.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"
#include <windows.h>
#include <string>
#include <vector>

namespace ravex::game {

namespace {

std::wstring javaDir() {
    return joinPath(ravex::kickxDir(), L"java");
}

bool fileExistsSimple(const std::wstring& path) {
    return GetFileAttributesW(path.c_str()) != INVALID_FILE_ATTRIBUTES;
}

bool tryJavaPath(const std::wstring& path, std::wstring& out) {
    std::wstring exe = joinPath(path, L"bin\\java.exe");
    if (fileExistsSimple(exe)) {
        out = exe;
        return true;
    }
    return false;
}

bool findSystemJava(int version, std::wstring& out) {
    DWORD homeSize = GetEnvironmentVariableW(L"JAVA_HOME", nullptr, 0);
    if (homeSize > 0) {
        std::wstring home(homeSize, L'\0');
        GetEnvironmentVariableW(L"JAVA_HOME", home.data(), homeSize);
        home.resize(homeSize - 1);
        if (tryJavaPath(home, out)) return true;
    }
    std::vector<std::wstring> searchPaths = {
        L"C:\\Program Files\\Eclipse Adoptium",
        L"C:\\Program Files\\Eclipse Foundation",
        L"C:\\Program Files\\Java",
        L"C:\\Program Files\\Microsoft\\jdk",
        L"C:\\Program Files (x86)\\Java",
    };
    for (const auto& base : searchPaths) {
        WIN32_FIND_DATAW fd;
        std::wstring pattern = base + L"\\*";
        HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
        if (hFind == INVALID_HANDLE_VALUE) continue;
        do {
            if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) continue;
            std::wstring name = fd.cFileName;
            if (name == L"." || name == L"..") continue;
            std::wstring candidate = joinPath(base, name);
            if (tryJavaPath(candidate, out)) return true;
        } while (FindNextFileW(hFind, &fd));
        FindClose(hFind);
    }
    return false;
}

bool downloadJava(int version, const std::function<void(const std::string&)>& status,
                  const bool* cancelled, std::wstring& outPath) {
    std::string error;
    std::string apiUrl = "https://api.adoptium.net/v3/assets/latest/" + std::to_string(version) +
                         "/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse";
    status("Fetching Java " + std::to_string(version) + " from Adoptium...");
    std::string json = net::httpGet(apiUrl, &error);
    if (json.empty()) {
        error = "Failed to fetch Adoptium API: " + error;
        return false;
    }
    ravex::json::Value root = ravex::json::Value::parse(json);
    if (root.isNull() || root.type() != ravex::json::Value::Type::Array || root.size() == 0) {
        error = "Invalid Adoptium response";
        return false;
    }
    const ravex::json::Value& first = root.at(0);
    if (!first.has("binary") || !first.at("binary").has("package")) {
        error = "Invalid Adoptium response structure";
        return false;
    }
    const ravex::json::Value& pkg = first.at("binary").at("package");
    std::string downloadUrl;
    if (pkg.has("link")) downloadUrl = pkg.at("link").asString();
    if (downloadUrl.empty()) {
        error = "No download URL in Adoptium response";
        return false;
    }
    std::wstring destDir = joinPath(javaDir(), fromUtf8(std::to_string(version)));
    createDirs(destDir);
    std::wstring zipPath = joinPath(destDir, L"jdk.zip");
    status("Downloading Java " + std::to_string(version) + "...");
    bool dlOk = net::downloadFile(downloadUrl, zipPath,
                                  [&status](const net::Progress& p) {
                                      if (p.total > 0) {
                                          int pct = static_cast<int>((p.downloaded * 100) / p.total);
                                          status("Downloading Java " + std::to_string(pct) + "%");
                                      }
                                  },
                                  cancelled, &error);
    if (!dlOk) return false;
    status("Extracting Java " + std::to_string(version) + "...");
    std::wstring extractCmd = L"tar -xf \"" + zipPath + L"\" -C \"" + destDir + L"\"";
    STARTUPINFOW si{};
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESHOWWINDOW;
    si.wShowWindow = SW_HIDE;
    PROCESS_INFORMATION pi{};
    std::vector<wchar_t> cmdBuf(extractCmd.begin(), extractCmd.end());
    cmdBuf.push_back(L'\0');
    if (!CreateProcessW(nullptr, cmdBuf.data(), nullptr, nullptr, FALSE,
                        CREATE_NO_WINDOW, nullptr, nullptr, &si, &pi)) {
        error = "Failed to extract Java archive";
        return false;
    }
    WaitForSingleObject(pi.hProcess, INFINITE);
    DWORD exitCode = 0;
    GetExitCodeProcess(pi.hProcess, &exitCode);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    if (exitCode != 0) {
        error = "Java extraction failed (exit code " + std::to_string(exitCode) + ")";
        return false;
    }
    DeleteFileW(zipPath.c_str());
    WIN32_FIND_DATAW fd;
    std::wstring pattern = joinPath(destDir, L"jdk-*");
    HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
    if (hFind != INVALID_HANDLE_VALUE) {
        std::wstring jdkDir = joinPath(destDir, fd.cFileName);
        FindClose(hFind);
        if (tryJavaPath(jdkDir, outPath)) return true;
    }
    if (tryJavaPath(destDir, outPath)) return true;
    error = "Java downloaded but java.exe not found";
    return false;
}

}

bool ensureJava(int version, std::wstring& outPath, std::string* error,
                const std::function<void(const std::string&)>& status,
                const bool* cancelled) {
    if (findSystemJava(version, outPath)) return true;
    std::wstring cachedPath = joinPath(joinPath(javaDir(), fromUtf8(std::to_string(version))), L"bin\\java.exe");
    if (fileExistsSimple(cachedPath)) {
        outPath = cachedPath;
        return true;
    }
    if (cancelled && *cancelled) {
        *error = "Cancelled";
        return false;
    }
    if (!downloadJava(version, status, cancelled, outPath)) {
        if (error) *error = "Java " + std::to_string(version) + " not found and download failed";
        return false;
    }
    return true;
}

}
