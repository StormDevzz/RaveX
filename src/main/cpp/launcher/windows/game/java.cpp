#include "game/include/java.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "core/include/sha256.hpp"
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

int parseJavaMajor(const std::string& output) {
    std::size_t vpos = output.find("version");
    std::size_t q1 = (vpos == std::string::npos) ? output.find('"') : output.find('"', vpos);
    if (q1 == std::string::npos) return 0;
    std::size_t q2 = output.find('"', q1 + 1);
    if (q2 == std::string::npos) return 0;
    std::string ver = output.substr(q1 + 1, q2 - q1 - 1);
    if (ver.size() > 2 && ver[0] == '1' && ver[1] == '.') {
        std::size_t dot = ver.find('.', 2);
        ver = ver.substr(2, dot == std::string::npos ? std::string::npos : dot - 2);
    }
    int major = 0;
    for (char c : ver) {
        if (c < '0' || c > '9') break;
        major = major * 10 + (c - '0');
        if (major > 1000) break;
    }
    return major;
}

int queryJavaMajor(const std::wstring& exe) {
    STARTUPINFOW si{};
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESHOWWINDOW | STARTF_USESTDHANDLES;
    si.wShowWindow = SW_HIDE;
    PROCESS_INFORMATION pi{};
    std::wstring cmd = L"\"" + exe + L"\" -version";
    std::vector<wchar_t> cmdBuf(cmd.begin(), cmd.end());
    cmdBuf.push_back(L'\0');
    HANDLE hRead = nullptr;
    HANDLE hWrite = nullptr;
    SECURITY_ATTRIBUTES sa{};
    sa.nLength = sizeof(sa);
    sa.bInheritHandle = TRUE;
    if (!CreatePipe(&hRead, &hWrite, &sa, 0)) return 0;
    SetHandleInformation(hRead, HANDLE_FLAG_INHERIT, 0);
    si.hStdInput = nullptr;
    si.hStdOutput = hWrite;
    si.hStdError = hWrite;
    if (!CreateProcessW(nullptr, cmdBuf.data(), nullptr, nullptr, TRUE, CREATE_NO_WINDOW,
                        nullptr, nullptr, &si, &pi)) {
        CloseHandle(hRead);
        CloseHandle(hWrite);
        return 0;
    }
    CloseHandle(hWrite);
    DWORD wait = WaitForSingleObject(pi.hProcess, 8000);
    if (wait == WAIT_TIMEOUT) TerminateProcess(pi.hProcess, 1);
    std::string output;
    char chunk[512];
    DWORD read = 0;
    while (ReadFile(hRead, chunk, static_cast<DWORD>(sizeof(chunk)), &read, nullptr) && read > 0) {
        output.append(chunk, read);
        if (output.size() > 65536) break;
    }
    CloseHandle(hRead);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    return parseJavaMajor(output);
}

std::vector<std::wstring> collectJavaCandidates() {
    std::vector<std::wstring> out;
    DWORD homeSize = GetEnvironmentVariableW(L"JAVA_HOME", nullptr, 0);
    if (homeSize > 0) {
        std::wstring home(homeSize, L'\0');
        GetEnvironmentVariableW(L"JAVA_HOME", home.data(), homeSize);
        home.resize(homeSize - 1);
        std::wstring exe = joinPath(home, L"bin\\java.exe");
        if (fileExistsSimple(exe)) out.push_back(exe);
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
            std::wstring exe = joinPath(joinPath(base, name), L"bin\\java.exe");
            if (fileExistsSimple(exe)) out.push_back(exe);
        } while (FindNextFileW(hFind, &fd));
        FindClose(hFind);
    }
    return out;
}

bool findCachedJava(int version, std::wstring& out) {
    std::wstring base = joinPath(javaDir(), fromUtf8(std::to_string(version)));
    if (tryJavaPath(base, out)) return true;
    WIN32_FIND_DATAW fd;
    // Adoptium JRE archives extract to jre-*, JDK archives to jdk-*
    for (const wchar_t* prefix : {L"jre-*", L"jdk-*"}) {
        std::wstring pattern = joinPath(base, prefix);
        HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
        if (hFind == INVALID_HANDLE_VALUE) continue;
        bool found = false;
        do {
            if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) continue;
            std::wstring candidate = joinPath(base, fd.cFileName);
            if (tryJavaPath(candidate, out)) {
                found = true;
                break;
            }
        } while (FindNextFileW(hFind, &fd));
        FindClose(hFind);
        if (found) return true;
    }
    return false;
}

bool findSystemJava(int version, std::wstring& out) {
    std::vector<std::wstring> candidates = collectJavaCandidates();
    std::wstring newer;
    int newerMajor = 0;
    for (const auto& exe : candidates) {
        int major = queryJavaMajor(exe);
        if (major <= 0) continue;
        if (major == version) {
            out = exe;
            return true;
        }
        if (major > version && (newer.empty() || major < newerMajor)) {
            newer = exe;
            newerMajor = major;
        }
    }
    if (!newer.empty()) {
        out = newer;
        return true;
    }
    return false;
}

bool downloadJava(int version, const std::function<void(const std::string&)>& status,
                  const bool* cancelled, std::wstring& outPath) {
    std::string error;
    const std::string versionText = std::to_string(version);
    // Adoptium dropped Windows 7/8 support, so those systems use the Liberica
    // JRE instead. Liberica still publishes Java 21 builds for Windows 7 SP1
    // and newer, which keeps old machines able to launch the game.
    const bool legacyWindows = isLegacyWindows();
    // A runtime is enough to launch the game. The JRE archive is roughly a
    // third of the JDK size, so a fresh install downloads far less.
    std::string apiUrl;
    if (legacyWindows) {
        apiUrl = "https://api.bell-sw.com/v1/liberica/releases?version-feature=" + versionText +
                 "&version-modifier=latest&bitness=64&os=windows&arch=x86&package-type=zip&bundle-type=jre";
    } else {
        apiUrl = "https://api.adoptium.net/v3/assets/latest/" + versionText +
                 "/hotspot?architecture=x64&image_type=jre&os=windows&vendor=eclipse";
    }
    const char* vendor = legacyWindows ? "Liberica" : "Adoptium";
    status(std::string("Fetching Java ") + versionText + " from " + vendor + "...");
    std::string json = net::httpGet(apiUrl, &error);
    if (json.empty()) {
        error = std::string("Failed to fetch ") + vendor + " API: " + error;
        return false;
    }
    ravex::json::Value root = ravex::json::Value::parse(json);
    if (root.isNull() || root.type() != ravex::json::Value::Type::Array || root.size() == 0) {
        error = std::string("Invalid ") + vendor + " response";
        return false;
    }
    const ravex::json::Value& first = root.at(0);
    std::string downloadUrl;
    std::string expectedHash;
    bool hashIsSha1 = false;
    if (legacyWindows) {
        if (first.has("downloadUrl")) downloadUrl = first.at("downloadUrl").asString();
        if (first.has("sha1")) {
            expectedHash = first.at("sha1").asString();
            hashIsSha1 = true;
        }
    } else {
        if (!first.has("binary") || !first.at("binary").has("package")) {
            error = "Invalid Adoptium response structure";
            return false;
        }
        const ravex::json::Value& pkg = first.at("binary").at("package");
        if (pkg.has("link")) downloadUrl = pkg.at("link").asString();
        if (pkg.has("checksum")) expectedHash = pkg.at("checksum").asString();
    }
    if (downloadUrl.empty()) {
        error = std::string("No download URL in ") + vendor + " response";
        return false;
    }
    std::wstring destDir = joinPath(javaDir(), fromUtf8(versionText));
    createDirs(destDir);
    std::wstring zipPath = joinPath(destDir, L"java.zip");
    status("Downloading Java " + versionText + "...");
    bool dlOk = net::downloadFile(downloadUrl, zipPath,
                                  [&status](const net::Progress& p) {
                                      if (p.total > 0) {
                                          int pct = static_cast<int>((p.downloaded * 100) / p.total);
                                          if (pct > 100) pct = 100;
                                          status("Downloading Java " + std::to_string(pct) + "%");
                                      }
                                  },
                                  cancelled, &error);
    if (!dlOk) return false;
    for (char& c : expectedHash) {
        if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
    }
    if (!expectedHash.empty()) {
        std::string actual = hashIsSha1 ? sha1File(zipPath) : sha256File(zipPath);
        if (actual.empty() || actual != expectedHash) {
            DeleteFileW(zipPath.c_str());
            error = "Java archive checksum mismatch";
            return false;
        }
    }
    status("Extracting Java " + versionText + "...");
    if (!extractZip(zipPath, destDir, &error)) {
        DeleteFileW(zipPath.c_str());
        return false;
    }
    DeleteFileW(zipPath.c_str());
    WIN32_FIND_DATAW fd;
    for (const wchar_t* prefix : {L"jre-*", L"jdk-*"}) {
        std::wstring pattern = joinPath(destDir, prefix);
        HANDLE hFind = FindFirstFileW(pattern.c_str(), &fd);
        if (hFind == INVALID_HANDLE_VALUE) continue;
        std::wstring jdkDir = joinPath(destDir, fd.cFileName);
        FindClose(hFind);
        if (tryJavaPath(jdkDir, outPath) && queryJavaMajor(outPath) >= version) return true;
    }
    if (tryJavaPath(destDir, outPath) && queryJavaMajor(outPath) >= version) return true;
    error = "Java downloaded but does not run on this system";
    return false;
}

}

bool ensureJava(int version, std::wstring& outPath, std::string* error,
                const std::function<void(const std::string&)>& status,
                const bool* cancelled) {
    std::string localError;
    if (!error) error = &localError;
    // Cached runtime first: this is the common reinstall case and avoids
    // spawning java -version for every JDK found on the machine.
    std::wstring cachedPath;
    if (findCachedJava(version, cachedPath) && queryJavaMajor(cachedPath) >= version) {
        outPath = cachedPath;
        return true;
    }
    if (findSystemJava(version, outPath)) return true;
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

std::string getBundledJavaVersion() {
    for (int v : {21, 17, 16}) {
        std::wstring cachedPath;
        if (findCachedJava(v, cachedPath)) {
            STARTUPINFOW si{}; si.cb = sizeof(si); si.dwFlags = STARTF_USESHOWWINDOW | STARTF_USESTDHANDLES; si.wShowWindow = SW_HIDE;
            PROCESS_INFORMATION pi{};
            std::wstring cmd = L"\"" + cachedPath + L"\" -version";
            std::vector<wchar_t> cmdBuf(cmd.begin(), cmd.end()); cmdBuf.push_back(L'\0');
            HANDLE hRead, hWrite;
            SECURITY_ATTRIBUTES sa{}; sa.nLength = sizeof(sa); sa.bInheritHandle = TRUE;
            if (!CreatePipe(&hRead, &hWrite, &sa, 0)) return "Java " + std::to_string(v);
            SetHandleInformation(hRead, HANDLE_FLAG_INHERIT, 0);
            si.hStdOutput = hWrite; si.hStdError = hWrite;
            if (CreateProcessW(nullptr, cmdBuf.data(), nullptr, nullptr, TRUE, CREATE_NO_WINDOW, nullptr, nullptr, &si, &pi)) {
                CloseHandle(hWrite);
                char buf[512]{}; DWORD read = 0;
                ReadFile(hRead, buf, sizeof(buf) - 1, &read, nullptr);
                CloseHandle(hRead); CloseHandle(pi.hProcess); CloseHandle(pi.hThread);
                std::string output(buf);
                size_t pos = output.find('"');
                if (pos != std::string::npos) {
                    size_t end = output.find('"', pos + 1);
                    if (end != std::string::npos) return output.substr(pos + 1, end - pos - 1);
                }
                return "Java " + std::to_string(v);
            }
            CloseHandle(hWrite); CloseHandle(hRead);
            return "Java " + std::to_string(v);
        }
    }
    std::wstring sysPath;
    if (findSystemJava(21, sysPath) || findSystemJava(17, sysPath)) {
        STARTUPINFOW si{}; si.cb = sizeof(si); si.dwFlags = STARTF_USESHOWWINDOW | STARTF_USESTDHANDLES; si.wShowWindow = SW_HIDE;
        PROCESS_INFORMATION pi{};
        std::wstring cmd = L"\"" + sysPath + L"\" -version";
        std::vector<wchar_t> cmdBuf(cmd.begin(), cmd.end()); cmdBuf.push_back(L'\0');
        HANDLE hRead, hWrite;
        SECURITY_ATTRIBUTES sa{}; sa.nLength = sizeof(sa); sa.bInheritHandle = TRUE;
        if (CreatePipe(&hRead, &hWrite, &sa, 0)) {
            SetHandleInformation(hRead, HANDLE_FLAG_INHERIT, 0);
            si.hStdOutput = hWrite; si.hStdError = hWrite;
            if (CreateProcessW(nullptr, cmdBuf.data(), nullptr, nullptr, TRUE, CREATE_NO_WINDOW, nullptr, nullptr, &si, &pi)) {
                CloseHandle(hWrite);
                char buf[512]{}; DWORD read = 0;
                ReadFile(hRead, buf, sizeof(buf) - 1, &read, nullptr);
                CloseHandle(hRead); CloseHandle(pi.hProcess); CloseHandle(pi.hThread);
                std::string output(buf);
                size_t pos = output.find('"');
                if (pos != std::string::npos) {
                    size_t end = output.find('"', pos + 1);
                    if (end != std::string::npos) return output.substr(pos + 1, end - pos - 1);
                }
                return "Java (system)";
            }
            CloseHandle(hWrite); CloseHandle(hRead);
        }
    }
    return "";
}

}
