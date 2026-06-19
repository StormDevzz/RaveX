#include "release_checker.h"
#include <sstream>
#include <algorithm>
#include <regex>
#include <cstdio>

namespace ravex {
namespace tools {
namespace github {

ReleaseChecker::ReleaseChecker(const std::string& owner, const std::string& repo)
    : m_owner(owner), m_repo(repo)
{}

CheckResult ReleaseChecker::checkForUpdates(const std::string& localVersion) {
    CheckResult result;
    result.localVersion = localVersion;

    auto release = m_api.getLatestRelease(m_owner, m_repo);
    if (release.tagName.empty()) {
        result.error = true;
        result.errorMessage = "Failed to fetch latest release";
        return result;
    }

    result.remoteVersion = release.tagName;
    result.releaseUrl = release.htmlUrl;
    result.remoteVersion = release.tagName;

    // Clean versions for comparison
    auto clean = [](const std::string& v) -> std::string {
        std::string s = v;
        if (!s.empty() && (s[0] == 'v' || s[0] == 'V')) s = s.substr(1);
        return s;
    };

    std::string local = clean(localVersion);
    std::string remote = clean(result.remoteVersion);

    // Simple dot-separated comparison
    auto parseParts = [](const std::string& v) {
        std::vector<int> parts;
        std::stringstream ss(v);
        std::string part;
        while (std::getline(ss, part, '.')) {
            try { parts.push_back(std::stoi(part)); }
            catch (...) { parts.push_back(0); }
        }
        return parts;
    };

    auto localParts = parseParts(local);
    auto remoteParts = parseParts(remote);

    size_t maxLen = std::max(localParts.size(), remoteParts.size());
    localParts.resize(maxLen, 0);
    remoteParts.resize(maxLen, 0);

    for (size_t i = 0; i < maxLen; i++) {
        if (remoteParts[i] > localParts[i]) {
            result.updateAvailable = true;
            break;
        }
        if (remoteParts[i] < localParts[i]) break;
    }

    // Get assets
    if (!release.tagName.empty()) {
        auto allReleases = m_api.listReleases(m_owner, m_repo, 1);
        // For simplicity, just indicate availability
    }

    return result;
}

bool ReleaseChecker::downloadUpdate(const CheckResult& result, const std::string& outputDir,
                                   std::function<void(int)> progressCallback)
{
    (void)progressCallback;
    if (result.error || result.releaseUrl.empty()) return false;

    // Use curl to download the release tarball
    std::string url = "https://api.github.com/repos/" + m_owner + "/" + m_repo
                    + "/releases/latest";
    std::string outPath = outputDir + "/ravex-" + result.remoteVersion + ".jar";

    (void)url;
    (void)outPath;

    // Placeholder — actual download uses the release assets
    return false;
}

std::vector<GithubRelease> ReleaseChecker::getAllReleases(int count) {
    return m_api.listReleases(m_owner, m_repo, count);
}

} // namespace github
} // namespace tools
} // namespace ravex
