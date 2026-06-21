#pragma once

#include "github_types.h"
#include <string>
#include <vector>
#include <memory>

namespace ravex { namespace github {
    class HttpClient;
} }

namespace ravex {
namespace github {

// ─── Release checker (semver comparison + GitHub API) ────────────────────────

class ReleaseChecker {
public:
    ReleaseChecker(const std::string& owner, const std::string& repo,
                   const std::string& token = "");
    ~ReleaseChecker();
    ReleaseChecker(ReleaseChecker&&) noexcept;
    ReleaseChecker& operator=(ReleaseChecker&&) noexcept;

    // Check if a newer version is available
    UpdateInfo checkForUpdates(const std::string& currentVersion,
                               ReleaseChannel channel = ReleaseChannel::Stable);

    // List recent releases
    std::vector<GithubRelease> listReleases(int count = 10,
                                            ReleaseChannel channel = ReleaseChannel::All);

    // Get a specific release
    GithubRelease getLatestRelease();
    GithubRelease getReleaseByTag(const std::string& tag);

    // Get repository info
    GithubRepo getRepoInfo();

    // Set callbacks
    void setLogCallback(LogCallback cb);

    // Access underlying HTTP client (for custom requests)
    HttpClient& http() const;

    // Errors
    std::string lastError() const;
    bool hasError() const;

private:
    std::unique_ptr<class ReleaseCheckerImpl> m_impl;
};

} // namespace github
} // namespace ravex
