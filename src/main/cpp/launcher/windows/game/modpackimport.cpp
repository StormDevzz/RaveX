#include "game/include/modpackimport.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"
#include <windows.h>
#include <atomic>
#include <mutex>
#include <thread>

namespace ravex::game {

namespace {

std::wstring tempExtractDir() {
    wchar_t tmpPath[MAX_PATH];
    GetTempPathW(MAX_PATH, tmpPath);
    std::wstring dir = joinPath(std::wstring(tmpPath), L"ravex_modpack_extract");
    createDirs(dir);
    return dir;
}

bool extractZipWithTar(const std::wstring& zipPath, const std::wstring& destDir, std::string* error) {
    createDirs(destDir);
    STARTUPINFOW si{};
    si.cb = sizeof(si);
    PROCESS_INFORMATION pi{};
    std::wstring cmd = L"tar -xf \"" + zipPath + L"\" -C \"" + destDir + L"\"";
    std::vector<wchar_t> cmdBuf(cmd.begin(), cmd.end());
    cmdBuf.push_back(0);
    if (!CreateProcessW(nullptr, cmdBuf.data(), nullptr, nullptr, FALSE, CREATE_NO_WINDOW, nullptr, nullptr, &si, &pi)) {
        if (error) *error = "Failed to launch tar";
        return false;
    }
    WaitForSingleObject(pi.hProcess, 120000);
    DWORD exitCode = 0;
    GetExitCodeProcess(pi.hProcess, &exitCode);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    if (exitCode != 0) {
        if (error) *error = "tar extraction failed with code " + std::to_string(exitCode);
        return false;
    }
    return true;
}

void removeDirectoryRecursive(const std::wstring& dir) {
    WIN32_FIND_DATAW fd;
    std::wstring pat = joinPath(dir, L"*");
    HANDLE h = FindFirstFileW(pat.c_str(), &fd);
    if (h != INVALID_HANDLE_VALUE) {
        do {
            std::wstring name = fd.cFileName;
            if (name == L"." || name == L"..") continue;
            std::wstring child = joinPath(dir, name);
            if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                removeDirectoryRecursive(child);
            } else {
                DeleteFileW(child.c_str());
            }
        } while (FindNextFileW(h, &fd));
        FindClose(h);
    }
    RemoveDirectoryW(dir.c_str());
}

void copyDirectoryRecursive(const std::wstring& src, const std::wstring& dest) {
    createDirs(dest);
    WIN32_FIND_DATAW fd;
    std::wstring pat = joinPath(src, L"*");
    HANDLE h = FindFirstFileW(pat.c_str(), &fd);
    if (h != INVALID_HANDLE_VALUE) {
        do {
            std::wstring name = fd.cFileName;
            if (name == L"." || name == L"..") continue;
            std::wstring srcChild = joinPath(src, name);
            std::wstring destChild = joinPath(dest, name);
            if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                copyDirectoryRecursive(srcChild, destChild);
            } else {
                createDirs(dest.substr(0, dest.find_last_of(L"\\/")));
                CopyFileW(srcChild.c_str(), destChild.c_str(), FALSE);
            }
        } while (FindNextFileW(h, &fd));
        FindClose(h);
    }
}

void listFilesRecursive(const std::wstring& dir, std::vector<std::wstring>& out) {
    WIN32_FIND_DATAW fd;
    std::wstring pat = joinPath(dir, L"*");
    HANDLE h = FindFirstFileW(pat.c_str(), &fd);
    if (h != INVALID_HANDLE_VALUE) {
        do {
            std::wstring name = fd.cFileName;
            if (name == L"." || name == L"..") continue;
            std::wstring child = joinPath(dir, name);
            if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                listFilesRecursive(child, out);
            } else {
                out.push_back(child);
            }
        } while (FindNextFileW(h, &fd));
        FindClose(h);
    }
}

std::wstring parentDir(const std::wstring& path) {
    size_t pos = path.find_last_of(L"\\/");
    if (pos != std::wstring::npos) return path.substr(0, pos);
    return path;
}

bool downloadCurseForgeFile(int projectId, int fileId, const std::wstring& destDir, const std::function<void(const std::string&)>& status, std::string* error) {
    std::string url = "https://api.curseforge.com/v1/mods/" + std::to_string(projectId) + "/files/" + std::to_string(fileId);
    std::string resp = net::httpGet(url, error);
    if (resp.empty()) return false;
    ravex::json::Value root = ravex::json::Value::parse(resp);
    if (root.isNull() || !root.has("data")) {
        if (error) *error = "Invalid CurseForge API response";
        return false;
    }
    const ravex::json::Value& data = root.at("data");
    std::string downloadUrl;
    if (data.has("downloadUrl") && !data.at("downloadUrl").isNull()) {
        downloadUrl = data.at("downloadUrl").asString();
    }
    if (downloadUrl.empty()) {
        if (data.has("fileName")) {
            std::string fileName = data.at("fileName").asString();
            downloadUrl = "https://edge.forgecdn.net/files/" + std::to_string(fileId / 1000) + "/" + std::to_string(fileId % 1000) + "/" + fileName;
        }
    }
    if (downloadUrl.empty()) {
        if (error) *error = "No download URL for CurseForge file " + std::to_string(fileId);
        return false;
    }
    std::string fileName;
    if (data.has("fileName")) fileName = data.at("fileName").asString();
    if (fileName.empty()) fileName = "mod_" + std::to_string(projectId) + "_" + std::to_string(fileId) + ".jar";
    std::wstring destFile = joinPath(destDir, fromUtf8(fileName));
    if (!net::downloadFile(downloadUrl, destFile, nullptr, nullptr, error)) return false;
    return true;
}

}

bool importCurseForgeModpack(const std::wstring& zipPath, const std::wstring& destDir, std::string* error,
                             const std::function<void(const std::string&)>& status) {
    if (!fileExists(zipPath)) {
        if (error) *error = "Zip file not found";
        return false;
    }
    std::wstring extractDir = tempExtractDir();
    removeDirectoryRecursive(extractDir);
    createDirs(extractDir);
    if (status) status("Extracting modpack archive...");
    if (!extractZipWithTar(zipPath, extractDir, error)) {
        removeDirectoryRecursive(extractDir);
        return false;
    }
    std::wstring manifestPath;
    std::vector<std::wstring> extractedFiles;
    listFilesRecursive(extractDir, extractedFiles);
    for (const auto& f : extractedFiles) {
        std::wstring lower = f;
        for (auto& c : lower) c = (wchar_t)towlower(c);
        size_t pos = lower.find(L"manifest.json");
        if (pos != std::wstring::npos && pos + 13 == lower.size()) {
            manifestPath = f;
            break;
        }
    }
    if (manifestPath.empty()) {
        if (error) *error = "manifest.json not found in modpack";
        removeDirectoryRecursive(extractDir);
        return false;
    }
    std::string manifestText;
    if (!readFile(manifestPath, manifestText)) {
        if (error) *error = "Failed to read manifest.json";
        removeDirectoryRecursive(extractDir);
        return false;
    }
    ravex::json::Value manifest = ravex::json::Value::parse(manifestText);
    if (manifest.isNull()) {
        if (error) *error = "Invalid manifest.json";
        removeDirectoryRecursive(extractDir);
        return false;
    }
    if (status) status("Downloading mods from CurseForge...");
    std::wstring modsDir = joinPath(destDir, L"mods");
    createDirs(modsDir);
    if (manifest.has("files")) {
        const ravex::json::Value& files = manifest.at("files");
        for (size_t i = 0; i < files.size(); ++i) {
            const ravex::json::Value& file = files.at(i);
            int projectId = 0;
            int fileId = 0;
            if (file.has("projectID")) projectId = (int)file.at("projectID").asNumber();
            if (file.has("fileID")) fileId = (int)file.at("fileID").asNumber();
            if (projectId == 0 || fileId == 0) continue;
            std::string fileStatus = "Downloading mod " + std::to_string(i + 1) + "/" + std::to_string(files.size());
            if (status) status(fileStatus);
            if (!downloadCurseForgeFile(projectId, fileId, modsDir, status, error)) {
                removeDirectoryRecursive(extractDir);
                return false;
            }
        }
    }
    if (status) status("Copying overrides...");
    std::wstring overridesDir;
    for (const auto& f : extractedFiles) {
        size_t pos = f.find(L"overrides");
        if (pos != std::wstring::npos) {
            std::wstring after = f.substr(pos + 10);
            if (after.empty() || after == L"\\" || after == L"/") {
                WIN32_FILE_ATTRIBUTE_DATA fad{};
                if (GetFileAttributesExW(f.c_str(), GetFileExInfoStandard, &fad)) {
                    if (fad.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                        overridesDir = f;
                        break;
                    }
                }
            }
        }
    }
    if (!overridesDir.empty()) {
        WIN32_FIND_DATAW fd;
        std::wstring pat = joinPath(overridesDir, L"*");
        HANDLE h = FindFirstFileW(pat.c_str(), &fd);
        if (h != INVALID_HANDLE_VALUE) {
            do {
                std::wstring name = fd.cFileName;
                if (name == L"." || name == L"..") continue;
                std::wstring srcChild = joinPath(overridesDir, name);
                std::wstring destChild = joinPath(destDir, name);
                if (fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                    copyDirectoryRecursive(srcChild, destChild);
                } else {
                    CopyFileW(srcChild.c_str(), destChild.c_str(), FALSE);
                }
            } while (FindNextFileW(h, &fd));
            CloseHandle(h);
        }
    }
    removeDirectoryRecursive(extractDir);
    if (status) status("CurseForge modpack import complete");
    return true;
}

bool importModrinthModpack(const std::wstring& zipPath, const std::wstring& destDir, std::string* error,
                           const std::function<void(const std::string&)>& status) {
    if (!fileExists(zipPath)) {
        if (error) *error = "Zip file not found";
        return false;
    }
    std::wstring extractDir = tempExtractDir();
    removeDirectoryRecursive(extractDir);
    createDirs(extractDir);
    if (status) status("Extracting modpack archive...");
    if (!extractZipWithTar(zipPath, extractDir, error)) {
        removeDirectoryRecursive(extractDir);
        return false;
    }
    std::wstring indexPath;
    std::vector<std::wstring> extractedFiles;
    listFilesRecursive(extractDir, extractedFiles);
    for (const auto& f : extractedFiles) {
        std::wstring lower = f;
        for (auto& c : lower) c = (wchar_t)towlower(c);
        size_t pos = lower.find(L"modrinth.index.json");
        if (pos != std::wstring::npos && pos + 19 == lower.size()) {
            indexPath = f;
            break;
        }
    }
    if (indexPath.empty()) {
        if (error) *error = "modrinth.index.json not found in modpack";
        removeDirectoryRecursive(extractDir);
        return false;
    }
    std::string indexText;
    if (!readFile(indexPath, indexText)) {
        if (error) *error = "Failed to read modrinth.index.json";
        removeDirectoryRecursive(extractDir);
        return false;
    }
    ravex::json::Value root = ravex::json::Value::parse(indexText);
    if (root.isNull()) {
        if (error) *error = "Invalid modrinth.index.json";
        removeDirectoryRecursive(extractDir);
        return false;
    }
    if (status) status("Downloading modpack files...");
    if (root.has("files")) {
        const ravex::json::Value& files = root.at("files");
        for (size_t i = 0; i < files.size(); ++i) {
            const ravex::json::Value& file = files.at(i);
            std::string url;
            std::string filename;
            std::string path;
            if (file.has("url")) url = file.at("url").asString();
            if (file.has("filename")) filename = file.at("filename").asString();
            if (file.has("path")) path = file.at("path").asString();
            if (url.empty()) continue;
            if (filename.empty() && !path.empty()) {
                size_t lastSlash = path.find_last_of('/');
                if (lastSlash != std::string::npos) filename = path.substr(lastSlash + 1);
                else filename = path;
            }
            if (filename.empty()) {
                size_t lastSlash = url.find_last_of('/');
                if (lastSlash != std::string::npos) filename = url.substr(lastSlash + 1);
            }
            std::wstring destFile;
            if (!path.empty()) {
                destFile = joinPath(destDir, fromUtf8(path));
            } else {
                destFile = joinPath(destDir, fromUtf8(filename));
            }
            std::wstring destParent = parentDir(destFile);
            createDirs(destParent);
            std::string fileStatus = "Downloading file " + std::to_string(i + 1) + "/" + std::to_string(files.size());
            if (status) status(fileStatus);
            if (!net::downloadFile(url, destFile, nullptr, nullptr, error)) {
                removeDirectoryRecursive(extractDir);
                return false;
            }
        }
    }
    if (status) status("Processing modpack dependencies...");
    if (root.has("dependencies")) {
        const ravex::json::Value& deps = root.at("dependencies");
        std::vector<std::string> depKeys = deps.keys();
        for (const auto& key : depKeys) {
            const ravex::json::Value& dep = deps.at(key);
            if (dep.has("version")) {
                std::string depVersion = dep.at("version").asString();
                std::string projectType;
                if (dep.has("project_id")) {
                    (void)dep.at("project_id").asString();
                }
                if (key.find("forge") != std::string::npos || key.find("neoforge") != std::string::npos) {
                    projectType = "forge";
                } else if (key.find("fabric") != std::string::npos) {
                    projectType = "fabric";
                } else if (key.find("quilt") != std::string::npos) {
                    projectType = "quilt";
                }
                if (!projectType.empty() && !depVersion.empty()) {
                    if (status) status("Loader dependency: " + projectType + " " + depVersion);
                }
            }
        }
    }
    removeDirectoryRecursive(extractDir);
    if (status) status("Modrinth modpack import complete");
    return true;
}

}
