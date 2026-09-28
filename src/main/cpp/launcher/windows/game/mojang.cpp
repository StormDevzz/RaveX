#include "game/include/mojang.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "core/include/config.hpp"
#include "core/include/sha256.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"
#include <windows.h>
#include <atomic>
#include <mutex>
#include <thread>
#include <vector>

namespace ravex::game {

namespace {

std::wstring versionsDir() {
    return joinPath(minecraftDir(), L"versions");
}

std::wstring librariesDir() {
    return joinPath(minecraftDir(), L"libraries");
}

std::wstring assetsDir() {
    return joinPath(minecraftDir(), L"assets");
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

bool fetchVersionJson(const std::string& url, const std::string& version, ravex::json::Value& out, std::string* error) {
    std::string text = net::httpGet(url, error);
    if (text.empty()) return false;
    out = ravex::json::Value::parse(text);
    if (out.isNull()) return false;
    createDirs(joinPath(versionsDir(), fromUtf8(version)));
    writeFileAtomic(versionJson(version), text);
    return true;
}

std::string toLowerHex(std::string s) {
    for (char& c : s) {
        if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
    }
    return s;
}

bool verifySha1(const std::wstring& path, const std::string& expected) {
    if (expected.empty()) return true;
    std::string actual = sha1File(path);
    if (actual.empty()) return false;
    return actual == toLowerHex(expected);
}

struct DlJob {
    std::string url;
    std::wstring dest;
    std::string sha1;
};

bool downloadClientJar(const ravex::json::Value& versionData, const std::string& version,
                       const std::function<void(const net::Progress&)>& progress, const bool* cancelled, std::string* error) {
    std::wstring jar = clientJar(version);
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
    std::string sha1;
    if (client.has("sha1")) sha1 = client.at("sha1").asString();
    if (fileExists(jar)) {
        if (verifySha1(jar, sha1)) return true;
        DeleteFileW(jar.c_str());
    }
    createDirs(joinPath(versionsDir(), fromUtf8(version)));
    for (int attempt = 0; attempt < 2; ++attempt) {
        if (!net::downloadFile(url, jar, progress, cancelled, error)) return false;
        if (verifySha1(jar, sha1)) return true;
        DeleteFileW(jar.c_str());
    }
    *error = "Client jar hash mismatch";
    return false;
}

bool parallelDownload(const std::vector<DlJob>& jobs,
                      const std::function<void(const net::Progress&)>& progress, const bool* cancelled, std::string* error, int conc = 6) {
    if (jobs.empty()) return true;
    std::atomic<size_t> idx{0};
    std::atomic<bool> failed{false};
    std::mutex mtx;
    std::string firstErr;
    std::vector<std::thread> ths;
    for (size_t t = 0; t < conc; ++t) {
        ths.emplace_back([&]() {
            while (true) {
                size_t i = idx.fetch_add(1);
                if (i >= jobs.size()) break;
                if (cancelled && *cancelled) { failed = true; break; }
                if (failed.load()) break;
                const DlJob& job = jobs[i];
                std::string err;
                bool ok = net::downloadFile(job.url, job.dest, progress, cancelled, &err);
                if (ok && !job.sha1.empty() && !verifySha1(job.dest, job.sha1)) {
                    DeleteFileW(job.dest.c_str());
                    ok = net::downloadFile(job.url, job.dest, progress, cancelled, &err);
                    if (ok && !verifySha1(job.dest, job.sha1)) {
                        DeleteFileW(job.dest.c_str());
                        err = "Hash mismatch: " + toUtf8(job.dest);
                        ok = false;
                    }
                }
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
    std::vector<DlJob> jobs;
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
        std::string sha1;
        if (artifact.has("sha1")) sha1 = artifact.at("sha1").asString();
        if (fileExists(dest)) {
            if (verifySha1(dest, sha1)) continue;
            DeleteFileW(dest.c_str());
        }
        std::wstring dir = dest.substr(0, dest.find_last_of(L"\\/"));
        createDirs(dir);
        jobs.push_back({url, dest, sha1});
    }
    return parallelDownload(jobs, progress, cancelled, error, loadLauncherConfig().downloadThreads);
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
    std::vector<DlJob> jobs;
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
        jobs.push_back({url, assetFile, hash});
    }
    if (jobs.empty()) return true;
    if (!parallelDownload(jobs, progress, cancelled, error, loadLauncherConfig().downloadThreads)) return false;
    size_t missing = 0;
    for (auto& j : jobs) {
        if (!fileExists(j.dest)) ++missing;
        else {
            WIN32_FILE_ATTRIBUTE_DATA fad{};
            if (GetFileAttributesExW(j.dest.c_str(), GetFileExInfoStandard, &fad)) {
                ULONGLONG sz = (static_cast<ULONGLONG>(fad.nFileSizeHigh) << 32) | fad.nFileSizeLow;
                if (sz == 0) { DeleteFileW(j.dest.c_str()); ++missing; }
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
    if (!fetchVersionJson(versionUrl, version, versionData, error)) return false;
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
    {
        std::string vtext;
        if (readFile(versionJson(version), vtext)) {
            ravex::json::Value vdata = ravex::json::Value::parse(vtext);
            if (!vdata.isNull()) {
                if (vdata.has("downloads") && vdata.at("downloads").has("client")) {
                    const ravex::json::Value& client = vdata.at("downloads").at("client");
                    std::string sha1;
                    if (client.has("sha1")) sha1 = client.at("sha1").asString();
                    if (!sha1.empty() && !verifySha1(jar, sha1)) {
                        if (error) *error = "Client jar hash mismatch";
                        return false;
                    }
                }
                if (vdata.has("libraries")) {
                    const ravex::json::Value& libArr = vdata.at("libraries");
                    std::vector<std::wstring> libFiles;
                    std::vector<std::string> libHashes;
                    for (std::size_t i = 0; i < libArr.size(); ++i) {
                        const ravex::json::Value& lib = libArr.at(i);
                        if (!lib.has("downloads") || !lib.at("downloads").has("artifact")) continue;
                        const ravex::json::Value& artifact = lib.at("downloads").at("artifact");
                        std::string pth; std::string sha1;
                        if (artifact.has("path")) pth = artifact.at("path").asString();
                        if (artifact.has("sha1")) sha1 = artifact.at("sha1").asString();
                        if (pth.empty() || sha1.empty()) continue;
                        std::wstring f = joinPath(libs, fromUtf8(pth));
                        if (!fileExists(f)) continue;
                        libFiles.push_back(f);
                        libHashes.push_back(sha1);
                    }
                    size_t libSample = std::min<size_t>(20, libFiles.size());
                    for (size_t i = 0; i < libSample; ++i) {
                        size_t k = (libFiles.size() * i) / (libSample ? libSample : 1);
                        if (!verifySha1(libFiles[k], libHashes[k])) {
                            DeleteFileW(libFiles[k].c_str());
                            if (error) *error = "Library hash mismatch: " + toUtf8(libFiles[k]);
                            return false;
                        }
                    }
                }
            }
        }
    }
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
        else {
            std::wstring found = fileExists(f) ? f : f2;
            if (!verifySha1(found, hash)) {
                DeleteFileW(found.c_str());
                ++missing;
            }
        }
    }
    if (missing > toSample / 3) {
        if (error) *error = "Assets incomplete: sample missing " + std::to_string(missing) + "/" + std::to_string(toSample);
        return false;
    }
    return true;
}

}
