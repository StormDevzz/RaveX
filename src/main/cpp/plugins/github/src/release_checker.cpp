#include "ravex/github/release_checker.hpp"
#include "ravex/github/http_client.hpp"
#include "ravex/github/json.hpp"
#include <sstream>
#include <algorithm>
#include <regex>

namespace ravex {
namespace github {



std::string Version::toString() const {
    std::string s = std::to_string(major) + "." + std::to_string(minor) + "." + std::to_string(patch);
    if (!prerelease.empty()) s += "-" + prerelease;
    if (!buildMeta.empty()) s += "+" + buildMeta;
    return s;
}

Version Version::fromString(const std::string& str) {
    Version v;
    std::string s = str;

    if (!s.empty() && (s[0] == 'v' || s[0] == 'V')) s = s.substr(1);


    size_t plus = s.find('+');
    if (plus != std::string::npos) {
        v.buildMeta = s.substr(plus + 1);
        s = s.substr(0, plus);
    }


    size_t dash = s.find('-');
    if (dash != std::string::npos) {
        v.prerelease = s.substr(dash + 1);
        s = s.substr(0, dash);
    }


    std::istringstream ss(s);
    std::string part;
    int idx = 0;
    while (std::getline(ss, part, '.') && idx < 3) {
        try {
            int val = std::stoi(part);
            if (idx == 0) v.major = val;
            else if (idx == 1) v.minor = val;
            else if (idx == 2) v.patch = val;
        } catch (...) {}
        idx++;
    }
    return v;
}

int Version::compare(const Version& a, const Version& b) {
    if (a.major != b.major) return a.major < b.major ? -1 : 1;
    if (a.minor != b.minor) return a.minor < b.minor ? -1 : 1;
    if (a.patch != b.patch) return a.patch < b.patch ? -1 : 1;


    if (a.prerelease.empty() && !b.prerelease.empty()) return 1;
    if (!a.prerelease.empty() && b.prerelease.empty()) return -1;
    if (a.prerelease != b.prerelease) return a.prerelease < b.prerelease ? -1 : 1;

    return 0;
}



class ReleaseCheckerImpl {
public:
    std::string owner, repo;
    mutable HttpClient http;

    ReleaseCheckerImpl(const std::string& o, const std::string& r, const std::string& token)
        : owner(o), repo(r) {
        http.setToken(token);
        http.setUserAgent("RaveX-ReleaseChecker/1.0");
    }

    GithubRelease parseRelease(const JsonValue& j) const {
        GithubRelease rel;
        rel.id = j["id"].asInt();
        rel.tagName = j["tag_name"].asString();
        rel.targetCommitish = j["target_commitish"].asString();
        rel.name = j["name"].asString();
        rel.body = j["body"].asString();
        rel.prerelease = j["prerelease"].asBool();
        rel.draft = j["draft"].asBool();
        rel.publishedAt = j["published_at"].asString();
        rel.createdAt = j["created_at"].asString();
        rel.htmlUrl = j["html_url"].asString();
        rel.tarballUrl = j["tarball_url"].asString();
        rel.zipballUrl = j["zipball_url"].asString();


        if (j.has("assets") && j["assets"].isArray()) {
            for (const auto& a : j["assets"].asArray()) {
                GithubAsset asset;
                asset.id = a["id"].asInt();
                asset.name = a["name"].asString();
                asset.contentType = a["content_type"].asString();
                asset.size = a["size"].asInt();
                asset.browserDownloadUrl = a["browser_download_url"].asString();
                if (a.has("url")) asset.apiDownloadUrl = a["url"].asString();
                asset.createdAt = a["created_at"].asString();
                asset.downloadCount = (int)a["download_count"].asInt();
                if (a.has("label")) asset.label = a["label"].asString();
                rel.assets.push_back(std::move(asset));
            }
        }

        return rel;
    }
};



ReleaseChecker::ReleaseChecker(const std::string& owner, const std::string& repo,
                                const std::string& token)
    : m_impl(std::make_unique<ReleaseCheckerImpl>(owner, repo, token)) {}

ReleaseChecker::~ReleaseChecker() = default;
ReleaseChecker::ReleaseChecker(ReleaseChecker&&) noexcept = default;
ReleaseChecker& ReleaseChecker::operator=(ReleaseChecker&&) noexcept = default;

UpdateInfo ReleaseChecker::checkForUpdates(const std::string& currentVersion,
                                            ReleaseChannel channel) {
    UpdateInfo info;


    GithubRelease latest = getLatestRelease();
    if (latest.tagName.empty()) {
        info.error = true;
        info.errorMessage = "Failed to fetch latest release";
        return info;
    }

    info.localVersion = Version::fromString(currentVersion);
    info.remoteVersion = Version::fromString(latest.tagName);
    info.release = latest;


    if (channel == ReleaseChannel::Stable && latest.prerelease) {

        auto allReleases = listReleases(20, ReleaseChannel::All);
        for (const auto& rel : allReleases) {
            if (!rel.prerelease && !rel.draft) {
                info.remoteVersion = Version::fromString(rel.tagName);
                info.release = rel;
                break;
            }
        }
    }

    int cmp = Version::compare(info.remoteVersion, info.localVersion);
    info.available = (cmp > 0);


    std::string platformSuffix;
#ifdef _WIN32
    platformSuffix = "win";
#else
    platformSuffix = "linux";
#endif
    for (const auto& asset : info.release.assets) {
        std::string name = asset.name;
        std::transform(name.begin(), name.end(), name.begin(), ::tolower);
        if (name.find(platformSuffix) != std::string::npos ||
            name.find(".jar") != std::string::npos ||
            name.find(".dll") != std::string::npos ||
            name.find(".so") != std::string::npos) {
            info.matchingAssets.push_back(asset);
        }
    }

    return info;
}

std::vector<GithubRelease> ReleaseChecker::listReleases(int count, ReleaseChannel channel) {
    std::string url = "https://api.github.com/repos/" + m_impl->owner + "/" + m_impl->repo
                    + "/releases?per_page=" + std::to_string(count);
    auto resp = m_impl->http.get(url);
    if (resp.error || resp.statusCode != 200) return {};

    std::vector<GithubRelease> releases;
    auto json = JsonValue::parse(resp.body);
    if (!json.isArray()) return {};

    for (const auto& j : json.asArray()) {
        auto rel = m_impl->parseRelease(j);
        if (channel == ReleaseChannel::Stable && rel.prerelease) continue;
        if (rel.draft) continue;
        releases.push_back(std::move(rel));
    }
    return releases;
}

GithubRelease ReleaseChecker::getLatestRelease() {
    std::string url = "https://api.github.com/repos/" + m_impl->owner + "/" + m_impl->repo
                    + "/releases/latest";
    auto resp = m_impl->http.get(url);
    if (resp.error || resp.statusCode != 200) return {};

    auto json = JsonValue::parse(resp.body);
    return m_impl->parseRelease(json);
}

GithubRelease ReleaseChecker::getReleaseByTag(const std::string& tag) {
    std::string url = "https://api.github.com/repos/" + m_impl->owner + "/" + m_impl->repo
                    + "/releases/tags/" + tag;
    auto resp = m_impl->http.get(url);
    if (resp.error || resp.statusCode != 200) return {};

    auto json = JsonValue::parse(resp.body);
    return m_impl->parseRelease(json);
}

GithubRepo ReleaseChecker::getRepoInfo() {
    std::string url = "https://api.github.com/repos/" + m_impl->owner + "/" + m_impl->repo;
    auto resp = m_impl->http.get(url);
    if (resp.error || resp.statusCode != 200) return {};

    auto j = JsonValue::parse(resp.body);
    GithubRepo r;
    r.owner = m_impl->owner;
    r.name = m_impl->repo;
    r.fullName = j["full_name"].asString();
    r.description = j["description"].asString();
    r.htmlUrl = j["html_url"].asString();
    r.apiUrl = j["url"].asString();
    r.defaultBranch = j["default_branch"].asString();
    r.language = j["language"].asString();
    r.stars = (int)j["stargazers_count"].asInt();
    r.forks = (int)j["forks_count"].asInt();
    r.openIssues = (int)j["open_issues_count"].asInt();
    r.archived = j["archived"].asBool();
    r.pushedAt = j["pushed_at"].asString();
    if (j.has("license") && !j["license"].isNull()) {
        auto lic = j["license"];
        r.license = lic["name"].asString();
        if (r.license.empty())
            r.license = lic["spdx_id"].asString();
    }
    return r;
}

void ReleaseChecker::setLogCallback(LogCallback cb) { m_impl->http.setLogCallback(std::move(cb)); }
HttpClient& ReleaseChecker::http() const { return m_impl->http; }
std::string ReleaseChecker::lastError() const { return ""; }
bool ReleaseChecker::hasError() const { return false; }

}
}
