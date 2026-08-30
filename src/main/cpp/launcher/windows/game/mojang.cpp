#include "game/include/mojang.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"
#include <windows.h>
#include <atomic>
#include <mutex>
#include <thread>
#include <vector>

namespace ravex::game {

namespace {

std::wstring profileDir() {
    return joinPath(ravexDir(), L"..");
}

std::wstring gameDir() {
    return joinPath(profileDir(), L".minecraft");
}

std::wstring versionsDir() {
    return joinPath(gameDir(), L"versions");
}

std::wstring librariesDir() {
    return joinPath(gameDir(), L"libraries");
}

std::wstring assetsDir() {
    return joinPath(gameDir(), L"assets");
}

std::wstring clientJar(const std::string& version) {
    return joinPath(joinPath(versionsDir(), fromUtf8(version)), fromUtf8(version) + L".jar");
}

std::wstring versionJson(const std::string& version) {
    return joinPath(joinPath(versionsDir(), fromUtf8(version)), fromUtf8(version) + L".json");
}

std::string libraryPath(const std::string& name) {
    std::string path;
    std::size_t start = 0;
    for (std::size_t i = 0; i < name.size(); ++i) {
        if (name[i] == ':' || name[i] == '.') {
            if (!path.empty()) path += '/';
            path += name.substr(start, i - start);
            if (name[i] == ':') {
                path += '/';
                start = i + 1;
            }
        }
    }
    if (start < name.size()) {
        if (!path.empty()) path += '/';
        path += name.substr(start);
        std::size_t lastColon = name.rfind(':');
        if (lastColon != std::string::npos) {
            std::string artifact = name.substr(lastColon + 1);
            path += '/' + artifact;
        }
    }
    return path;
}

std::string manifestVersion = "17";

bool fetchVersionManifest(ravex::json::Value& out, std::string* error) {
    std::string url = "https://launchermeta.mojang.com/mc/game/version_manifest_v2.json";
    std::string text = net::httpGet(url, error);
    if (text.empty()) return false;
    out = ravex::json::Value::parse(text);
    return !out.isNull();
}

bool findVersionUrl(const ravex::json::Value& manifest, const std::string& version, std::string& url) {
    if (!manifest.has("versions")) return false;
    const ravex::json::Value& versions = manifest.at("versions");
    for (std::size_t i = 0; i < versions.size(); ++i) {
        const ravex::json::Value& v = versions.at(i);
        if (v.has("id") && v.at("id").asString() == version) {
            if (v.has("url")) {
                url = v.at("url").asString();
                return true;
            }
        }
    }
    return false;
}

bool fetchVersionJson(const std::string& url, ravex::json::Value& out, std::string* error) {
    std::string text = net::httpGet(url, error);
    if (text.empty()) return false;
    out = ravex::json::Value::parse(text);
    return !out.isNull();
}

bool downloadClientJar(const ravex::json::Value& versionData, const std::string& version,
                       const std::function<void(const net::Progress&)>& progress, const bool* cancelled, std::string* error) {
    std::wstring jar = clientJar(version);
    if (fileExists(jar)) return true;
    if (!versionData.has("downloads") || !versionData.at("downloads").has("client")) {
        *error = "No client download in version manifest";
        return false;
    }
    const ravex::json::Value& client = versionData.at("downloads").at("client");
    std::string url;
    if (client.has("url")) url = client.at("url").asString();
    if (url.empty()) {
        *error = "No client URL";
        return false;
    }
    createDirs(joinPath(versionsDir(), fromUtf8(version)));
    return net::downloadFile(url, jar, progress, cancelled, error);
}

bool parallelDownload(const std::vector<std::pair<std::string, std::wstring>>& jobs,
                      const std::function<void(const net::Progress&)>& progress, const bool* cancelled, std::string* error) {
    if (jobs.empty()) return true;
    std::atomic<size_t> idx{0};
    std::atomic<bool> failed{false};
    std::mutex mtx;
    std::string firstErr;
    const size_t conc = 6;
    std::vector<std::thread> ths;
    for (size_t t = 0; t < conc; ++t) {
        ths.emplace_back([&]() {
            while (true) {
                size_t i = idx.fetch_add(1);
                if (i >= jobs.size()) break;
                if (cancelled && *cancelled) { failed = true; break; }
                if (failed.load()) break;
                const auto& job = jobs[i];
                std::string err;
                bool ok = net::downloadFile(job.first, job.second, progress, cancelled, &err);
                if (!ok) {
                    std::lock_guard<std::mutex> lk(mtx);
                    if (!failed) firstErr = err;
                    failed = true;
                    break;
                }
            }
        });
    }
    for (auto& th : ths) th.join();
    if (failed) { *error = firstErr.empty() ? "Download failed" : firstErr; return false; }
    if (cancelled && *cancelled) { *error = "Cancelled"; return false; }
    return true;
}

bool downloadLibraries(const ravex::json::Value& versionData,
                       const std::function<void(const net::Progress&)>& progress, const bool* cancelled, std::string* error) {
    if (!versionData.has("libraries")) return true;
    const ravex::json::Value& libs = versionData.at("libraries");
    std::wstring libsDir = librariesDir();
    createDirs(libsDir);
    std::vector<std::pair<std::string, std::wstring>> jobs;
    for (std::size_t i = 0; i < libs.size(); ++i) {
        if (cancelled && *cancelled) { *error = "Cancelled"; return false; }
        const ravex::json::Value& lib = libs.at(i);
        if (!lib.has("downloads") || !lib.at("downloads").has("artifact")) continue;
        const ravex::json::Value& artifact = lib.at("downloads").at("artifact");
        std::string url; std::string path;
        if (artifact.has("url")) url = artifact.at("url").asString();
        if (artifact.has("path")) path = artifact.at("path").asString();
        if (url.empty()) continue;
        std::wstring dest;
        if (!path.empty()) dest = joinPath(libsDir, fromUtf8(path));
        else {
            std::string name; if (lib.has("name")) name = lib.at("name").asString();
            dest = joinPath(libsDir, fromUtf8(libraryPath(name)));
        }
        if (fileExists(dest)) continue;
        std::wstring dir = dest.substr(0, dest.find_last_of(L"\\/"));
        createDirs(dir);
        jobs.emplace_back(url, dest);
    }
    return parallelDownload(jobs, progress, cancelled, error);
}

bool downloadAssetIndex(const ravex::json::Value& versionData, std::string& indexId,
                        const std::function<void(const net::Progress&)>& progress, const bool* cancelled, std::string* error) {
    if (!versionData.has("assetIndex")) {
        indexId = "legacy";
        return true;
    }
    const ravex::json::Value& idx = versionData.at("assetIndex");
    if (idx.has("id")) indexId = idx.at("id").asString();
    if (indexId.empty()) {
        if (error) *error = "Empty asset index id";
        return false;
    }
    std::wstring idxPath = joinPath(assetsDir(), L"indexes");
    createDirs(idxPath);
    std::wstring idxFile = joinPath(idxPath, fromUtf8(indexId) + L".json");
    if (fileExists(idxFile)) {
        WIN32_FILE_ATTRIBUTE_DATA fad{};
        if (GetFileAttributesExW(idxFile.c_str(), GetFileExInfoStandard, &fad)) {
            ULONGLONG sz = (static_cast<ULONGLONG>(fad.nFileSizeHigh) << 32) | fad.nFileSizeLow;
            if (sz > 512) return true;
            DeleteFileW(idxFile.c_str());
        } else return true;
    }
    std::string url;
    if (idx.has("url")) url = idx.at("url").asString();
    if (url.empty()) {
        if (error) *error = "No asset index URL";
        return false;
    }
    if (!net::downloadFile(url, idxFile, progress, cancelled, error)) return false;
    WIN32_FILE_ATTRIBUTE_DATA fad{};
    if (!GetFileAttributesExW(idxFile.c_str(), GetFileExInfoStandard, &fad)) {
        if (error) *error = "Asset index download failed";
        return false;
    }
    ULONGLONG sz = (static_cast<ULONGLONG>(fad.nFileSizeHigh) << 32) | fad.nFileSizeLow;
    if (sz < 512) {
        if (error) *error = "Asset index too small";
        DeleteFileW(idxFile.c_str());
        return false;
    }
    return true;
}

bool downloadAssets(const std::string& indexId, const std::function<void(const net::Progress&)>& progress,
                    const bool* cancelled, std::string* error) {
    std::wstring idxFile = joinPath(joinPath(assetsDir(), L"indexes"), fromUtf8(indexId) + L".json");
    std::string text;
    if (!readFile(idxFile, text)) {
        if (error) *error = "Asset index not found: " + indexId;
        return false;
    }
    ravex::json::Value root = ravex::json::Value::parse(text);
    if (root.isNull() || !root.has("objects")) {
        if (error) *error = "Invalid asset index: " + indexId;
        return false;
    }
    const ravex::json::Value& objects = root.at("objects");
    std::vector<std::string> keys = objects.keys();
    if (keys.empty()) {
        if (error) *error = "Empty asset index: " + indexId;
        return false;
    }
    std::wstring objectsDir = assetsDir();
    std::vector<std::pair<std::string, std::wstring>> jobs;
    size_t existing = 0;
    for (std::size_t i = 0; i < keys.size(); ++i) {
        if (cancelled && *cancelled) { *error = "Cancelled"; return false; }
        const ravex::json::Value& obj = objects.at(keys[i]);
        if (!obj.has("hash")) continue;
        std::string hash = obj.at("hash").asString();
        if (hash.size() < 2) continue;
        std::string prefix = hash.substr(0, 2);
        std::wstring assetDir = joinPath(joinPath(objectsDir, L"objects"), fromUtf8(prefix));
        std::wstring assetFile = joinPath(assetDir, fromUtf8(hash));
        if (fileExists(assetFile)) {
            WIN32_FILE_ATTRIBUTE_DATA fad{};
            if (GetFileAttributesExW(assetFile.c_str(), GetFileExInfoStandard, &fad)) {
                ULONGLONG sz = (static_cast<ULONGLONG>(fad.nFileSizeHigh) << 32) | fad.nFileSizeLow;
                if (sz > 0) { ++existing; continue; }
                DeleteFileW(assetFile.c_str());
            } else { ++existing; continue; }
        }
        createDirs(assetDir);
        std::string url = "https://resources.download.minecraft.net/" + prefix + "/" + hash;
        jobs.emplace_back(url, assetFile);
    }
    if (jobs.empty()) return true;
    if (!parallelDownload(jobs, progress, cancelled, error)) return false;
    size_t missing = 0;
    for (auto& j : jobs) {
        if (!fileExists(j.second)) ++missing;
        else {
            WIN32_FILE_ATTRIBUTE_DATA fad{};
            if (GetFileAttributesExW(j.second.c_str(), GetFileExInfoStandard, &fad)) {
                ULONGLONG sz = (static_cast<ULONGLONG>(fad.nFileSizeHigh) << 32) | fad.nFileSizeLow;
                if (sz == 0) { DeleteFileW(j.second.c_str()); ++missing; }
            }
        }
    }
    if (missing > 0) {
        if (error) *error = "Assets incomplete: " + std::to_string(missing) + "/" + std::to_string(jobs.size()) + " failed";
        return false;
    }
    return true;
}

}

bool ensureMinecraft(const std::string& version, std::string* error,
                      const std::function<void(const net::Progress&)>& progress, const bool* cancelled, std::string* outIndexId) {
    if (version.empty()) {
        *error = "No version specified";
        return false;
    }
    ravex::json::Value manifest;
    if (!fetchVersionManifest(manifest, error)) return false;
    std::string versionUrl;
    if (!findVersionUrl(manifest, version, versionUrl)) {
        *error = "Version " + version + " not found";
        return false;
    }
    ravex::json::Value versionData;
    if (!fetchVersionJson(versionUrl, versionData, error)) return false;
    if (!downloadClientJar(versionData, version, progress, cancelled, error)) return false;
    if (!downloadLibraries(versionData, progress, cancelled, error)) return false;
    std::string indexId;
    if (!downloadAssetIndex(versionData, indexId, progress, cancelled, error)) return false;
    if (!downloadAssets(indexId, progress, cancelled, error)) return false;
    if (outIndexId) *outIndexId = indexId;
    return true;
}

bool isMinecraftInstalled(const std::string& version) {
    if (version.empty()) return false;
    return fileExists(clientJar(version));
}

std::string getFallbackAssetIndexId(const std::string& version) {
    wchar_t profile[MAX_PATH];
    DWORD len = GetEnvironmentVariableW(L"USERPROFILE", profile, MAX_PATH);
    if (len == 0 || len >= MAX_PATH) return version;
    std::wstring idxDir = joinPath(joinPath(joinPath(std::wstring(profile), L".minecraft"), L"assets"), L"indexes");
    WIN32_FIND_DATAW fd;
    HANDLE h = FindFirstFileW((idxDir + L"\\*.json").c_str(), &fd);
    if (h != INVALID_HANDLE_VALUE) {
        std::wstring name = fd.cFileName;
        FindClose(h);
        size_t dot = name.find_last_of(L'.');
        if (dot != std::wstring::npos) name = name.substr(0, dot);
        std::string id = toUtf8(name);
        if (!id.empty()) return id;
    }
    return version;
}

bool quickIntegrityCheck(const std::string& version, const std::string& assetIndexId, std::string* error) {
    wchar_t profile[MAX_PATH];
    DWORD l = GetEnvironmentVariableW(L"USERPROFILE", profile, MAX_PATH);
    if (l == 0 || l >= MAX_PATH) { if (error) *error = "No profile"; return false; }
    std::wstring p = profile;
    std::wstring jar = joinPath(joinPath(joinPath(joinPath(p, L".minecraft"), L"versions"), fromUtf8(version)), fromUtf8(version) + L".jar");
    if (!fileExists(jar)) { if (error) *error = "Client jar missing"; return false; }
    WIN32_FILE_ATTRIBUTE_DATA fad{};
    if (GetFileAttributesExW(jar.c_str(), GetFileExInfoStandard, &fad)) {
        ULONGLONG sz = (static_cast<ULONGLONG>(fad.nFileSizeHigh) << 32) | fad.nFileSizeLow;
        if (sz < 1024 * 10) { if (error) *error = "Client jar too small/empty"; return false; }
    }
    std::wstring libs = joinPath(joinPath(p, L".minecraft"), L"libraries");
    if (!fileExists(libs)) { if (error) *error = "Libraries missing"; return false; }
    WIN32_FIND_DATAW fd{};
    HANDLE hLib = FindFirstFileW(joinPath(libs, L"*").c_str(), &fd);
    if (hLib == INVALID_HANDLE_VALUE) { if (error) *error = "Libraries empty"; return false; }
    FindClose(hLib);
    std::string idx = assetIndexId.empty() ? getFallbackAssetIndexId(version) : assetIndexId;
    std::wstring idxFile = joinPath(joinPath(joinPath(joinPath(p, L".minecraft"), L"assets"), L"indexes"), fromUtf8(idx) + L".json");
    if (!fileExists(idxFile)) { if (error) *error = "Asset index missing " + idx; return false; }
    if (GetFileAttributesExW(idxFile.c_str(), GetFileExInfoStandard, &fad)) {
        ULONGLONG sz = (static_cast<ULONGLONG>(fad.nFileSizeHigh) << 32) | fad.nFileSizeLow;
        if (sz < 512) { if (error) *error = "Asset index empty"; return false; }
    }
    std::string text;
    if (!readFile(idxFile, text)) { if (error) *error = "Cannot read asset index"; return false; }
    ravex::json::Value root = ravex::json::Value::parse(text);
    if (root.isNull() || !root.has("objects")) { if (error) *error = "Invalid asset index json"; return false; }
    const ravex::json::Value& objects = root.at("objects");
    auto keys = objects.keys();
    if (keys.empty()) { if (error) *error = "Asset index has no objects"; return false; }
    size_t toSample = std::min<size_t>(30, keys.size());
    size_t missing = 0;
    for (size_t i = 0; i < toSample; ++i) {
        size_t idxSample = (keys.size() * i) / toSample;
        const ravex::json::Value& obj = objects.at(keys[idxSample]);
        if (!obj.has("hash")) continue;
        std::string hash = obj.at("hash").asString();
        if (hash.size() < 2) continue;
        std::wstring f = joinPath(joinPath(joinPath(joinPath(p, L".minecraft"), L"assets"), L"objects"), fromUtf8(hash.substr(0,2) + "/" + hash));
        std::wstring f2 = joinPath(joinPath(joinPath(joinPath(joinPath(p, L".minecraft"), L"assets"), L"objects"), fromUtf8(hash.substr(0,2))), fromUtf8(hash));
        if (!fileExists(f) && !fileExists(f2)) ++missing;
    }
    if (missing > toSample / 3) {
        if (error) *error = "Assets incomplete: sample missing " + std::to_string(missing) + "/" + std::to_string(toSample);
        return false;
    }
    return true;
}

}
