#pragma once

#include "github_types.h"
#include <string>
#include <functional>

namespace ravex {
namespace tools {
namespace github {

struct CheckResult {
    bool updateAvailable = false;
    std::string localVersion;
    std::string remoteVersion;
    std::string releaseUrl;
    std::vector<GithubAsset> assets;
    bool error = false;
    std::string errorMessage;
};

class ReleaseChecker {
public:
    ReleaseChecker(const std::string& owner, const std::string& repo);

    CheckResult checkForUpdates(const std::string& localVersion);
    bool downloadUpdate(const CheckResult& result, const std::string& outputDir,
                       std::function<void(int)> progressCallback = nullptr);

    std::vector<GithubRelease> getAllReleases(int count = 5);

private:
    std::string m_owner;
    std::string m_repo;
    GithubAPI m_api;
};

} // namespace github
} // namespace tools
} // namespace ravex
