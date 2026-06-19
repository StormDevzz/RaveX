#pragma once

#include "github_types.h"
#include <string>
#include <functional>

namespace ravex {
namespace tools {
namespace github {

class GithubAPI {
public:
    GithubAPI(const std::string& token = "");

    void setToken(const std::string& token);
    void setUserAgent(const std::string& ua);

    // Repository info
    GithubRepo getRepo(const std::string& owner, const std::string& repo);

    // Releases
    std::vector<GithubRelease> listReleases(const std::string& owner, const std::string& repo, int perPage = 10);
    GithubRelease getLatestRelease(const std::string& owner, const std::string& repo);
    GithubRelease getReleaseByTag(const std::string& owner, const std::string& repo, const std::string& tag);

    // Assets
    std::vector<GithubAsset> listAssets(const std::string& owner, const std::string& repo, int releaseId);
    bool downloadAsset(const GithubAsset& asset, const std::string& outputPath);

    // Rate limit info
    int getRemainingRequests();
    int getRateLimit();

private:
    std::string m_token;
    std::string m_userAgent;
    std::string m_apiBase = "https://api.github.com";

    std::string request(const std::string& method, const std::string& path,
                       const std::string& body = "",
                       HeaderMap extraHeaders = HeaderMap());

    HeaderMap defaultHeaders();
};

} // namespace github
} // namespace tools
} // namespace ravex
