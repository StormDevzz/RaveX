#include "game/include/ravex.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"

namespace ravex::game {

namespace {

std::wstring ravexJar() {
    return joinPath(modsDir(), L"RaveX.jar");
}

std::string getLatestReleaseUrl(std::string* error) {
    std::string body = net::httpGet("https://api.github.com/repos/3000IQGames/RaveX/releases/latest", error);
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
    std::string body = net::httpGet("https://api.github.com/repos/3000IQGames/RaveX/releases/latest", error);
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
                std::string name = asset.at("name").asString();
                if (name.size() > 4 && name.substr(name.size() - 4) == ".jar") {
                    out->url = asset.at("browser_download_url").asString();
                    out->name = name;
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
    std::wstring dest = joinPath(dir, fromUtf8(release.name.empty() ? release.tag + ".jar" : release.name));
    return net::downloadFile(release.url, dest, progress, cancelled, error);
}

}
