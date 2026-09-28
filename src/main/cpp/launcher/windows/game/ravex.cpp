#include "game/include/ravex.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "core/include/sha256.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"
#include <windows.h>

namespace ravex::game {

namespace {

const char* kReleaseApi = "https://api.github.com/repos/StormDevzz/RaveX/releases/latest";

std::wstring ravexJar() {
    return joinPath(modsDir(), L"RaveX.jar");
}

std::string sanitizeAssetName(const std::string& name) {
    std::size_t slash = name.find_last_of("/\\");
    std::string base = (slash == std::string::npos) ? name : name.substr(slash + 1);
    if (base.empty()) return {};
    for (char c : base) {
        bool ok = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') ||
                  c == '.' || c == '-' || c == '_';
        if (!ok) return {};
    }
    return base;
}

void deleteStaleRavexJars(const std::wstring& dir) {
    WIN32_FIND_DATAW fd;
    HANDLE h = FindFirstFileW(joinPath(dir, L"*.jar").c_str(), &fd);
    if (h == INVALID_HANDLE_VALUE) return;
    do {
        std::wstring name = fd.cFileName;
        std::wstring lower = name;
        for (auto& c : lower) c = static_cast<wchar_t>(towlower(c));
        bool endsJar = lower.size() > 4 && lower.substr(lower.size() - 4) == L".jar";
        if (endsJar && (lower == L"ravex.jar" || lower.rfind(L"ravex-", 0) == 0)) {
            DeleteFileW(joinPath(dir, name).c_str());
        }
    } while (FindNextFileW(h, &fd));
    FindClose(h);
}

std::string toLowerHexStr(std::string s) {
    for (char& c : s) {
        if (c >= 'A' && c <= 'Z') c = static_cast<char>(c - 'A' + 'a');
    }
    return s;
}

std::string getLatestReleaseUrl(std::string* error) {
    std::string body = net::httpGet(kReleaseApi, error);
    if (body.empty()) return {};
    ravex::json::Value root = ravex::json::Value::parse(body);
    if (root.isNull()) {
        *error = "Failed to parse release JSON";
        return {};
    }
    if (root.has("tag_name")) return root.at("tag_name").asString();
    *error = "No tag_name in release";
    return {};
}

bool findAssetUrl(const ravex::json::Value& release, const std::string& name, std::string& url) {
    if (!release.has("assets")) return false;
    const ravex::json::Value& assets = release.at("assets");
    for (std::size_t i = 0; i < assets.size(); ++i) {
        const ravex::json::Value& asset = assets.at(i);
        if (asset.has("name") && asset.at("name").asString() == name) {
            if (asset.has("browser_download_url")) {
                url = asset.at("browser_download_url").asString();
                return true;
            }
        }
    }
    return false;
}

}

bool fetchLatestRelease(ReleaseInfo* out, std::string* error) {
    if (!out) return false;
    std::string body = net::httpGet(kReleaseApi, error);
    if (body.empty()) return false;
    ravex::json::Value root = ravex::json::Value::parse(body);
    if (root.isNull()) {
        *error = "Failed to parse release JSON";
        return false;
    }
    if (root.has("tag_name")) out->tag = root.at("tag_name").asString();
    if (root.has("name")) out->name = root.at("name").asString();
    if (root.has("assets")) {
        const ravex::json::Value& assets = root.at("assets");
        for (std::size_t i = 0; i < assets.size(); ++i) {
            const ravex::json::Value& asset = assets.at(i);
            if (asset.has("name") && asset.has("browser_download_url")) {
                std::string name = sanitizeAssetName(asset.at("name").asString());
                if (name.size() > 4 && name.substr(name.size() - 4) == ".jar") {
                    out->url = asset.at("browser_download_url").asString();
                    out->name = name;
                    if (asset.has("digest")) {
                        std::string digest = asset.at("digest").asString();
                        if (digest.rfind("sha256:", 0) == 0) out->sha256 = digest.substr(7);
                    }
                    break;
                }
            }
        }
    }
    if (out->tag.empty()) {
        *error = "No release tag found";
        return false;
    }
    return true;
}

bool installRavex(const ReleaseInfo& release, std::string* error,
                  const std::function<void(const net::Progress&)>& progress, const bool* cancelled) {
    if (release.url.empty()) {
        *error = "No download URL for release";
        return false;
    }
    std::wstring dir = modsDir();
    createDirs(dir);
    std::string safe;
    if (release.name.size() > 4 && release.name.substr(release.name.size() - 4) == ".jar") {
        safe = sanitizeAssetName(release.name);
    }
    if (safe.empty()) {
        std::string tagSafe = sanitizeAssetName(release.tag);
        if (!tagSafe.empty()) safe = tagSafe + ".jar";
    }
    if (safe.size() < 5) safe = "ravex-update.jar";
    deleteStaleRavexJars(dir);
    std::wstring dest = joinPath(dir, fromUtf8(safe));
    if (!net::downloadFile(release.url, dest, progress, cancelled, error)) return false;
    if (!release.sha256.empty()) {
        std::string actual = sha256File(dest);
        if (actual.empty() || actual != toLowerHexStr(release.sha256)) {
            DeleteFileW(dest.c_str());
            *error = "RaveX jar checksum mismatch";
            return false;
        }
    }
    return true;
}

}
