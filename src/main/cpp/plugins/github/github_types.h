#pragma once

#include <string>
#include <vector>
#include <map>

namespace ravex {
namespace tools {
namespace github {

struct GithubUser {
    std::string login;
    std::string avatarUrl;
    std::string htmlUrl;
    int publicRepos = 0;
};

struct GithubRelease {
    std::string tagName;
    std::string targetCommitish;
    std::string name;
    std::string body;
    bool prerelease = false;
    bool draft = false;
    std::string publishedAt;
    std::string htmlUrl;
    std::string tarballUrl;
    std::string zipballUrl;
};

struct GithubAsset {
    std::string name;
    std::string contentType;
    long size = 0;
    std::string downloadUrl;
    std::string createdAt;
    int downloadCount = 0;
};

struct GithubRepo {
    std::string owner;
    std::string name;
    std::string fullName;
    std::string description;
    std::string defaultBranch;
    std::string htmlUrl;
    std::string apiUrl;
    int stars = 0;
    int forks = 0;
    int openIssues = 0;
    bool archived = false;
    std::string pushedAt;
};

using HeaderMap = std::map<std::string, std::string>;

} // namespace github
} // namespace tools
} // namespace ravex
