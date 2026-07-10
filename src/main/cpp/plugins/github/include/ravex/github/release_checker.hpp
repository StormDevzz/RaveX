#pragma once

#include "github_types.hpp"
#include <string>
#include <vector>
#include <memory>

namespace ravex { namespace github {
    class HttpClient;
} }

namespace ravex {
namespace github {



class ReleaseChecker {
public:
    ReleaseChecker(const std::string& owner, const std::string& repo,
                   const std::string& token = "");
    ~ReleaseChecker();
    ReleaseChecker(ReleaseChecker&&) noexcept;
    ReleaseChecker& operator=(ReleaseChecker&&) noexcept;


    UpdateInfo checkForUpdates(const std::string& currentVersion,
                               ReleaseChannel channel = ReleaseChannel::Stable);


    std::vector<GithubRelease> listReleases(int count = 10,
                                            ReleaseChannel channel = ReleaseChannel::All);


    GithubRelease getLatestRelease();
    GithubRelease getReleaseByTag(const std::string& tag);


    GithubRepo getRepoInfo();


    void setLogCallback(LogCallback cb);


    HttpClient& http() const;


    std::string lastError() const;
    bool hasError() const;

private:
    std::unique_ptr<class ReleaseCheckerImpl> m_impl;
};

}
}
