#pragma once

#include <string>
#include <vector>
#include <map>
#include <chrono>
#include <functional>

namespace ravex {
namespace github {

// ─── Enums ───────────────────────────────────────────────────────────────────

enum class HttpMethod { GET, POST, PUT, DELETE };
enum class LogLevel  { Debug, Info, Warn, Error };
enum class ReleaseChannel { Stable, Prerelease, All };

// ─── Callbacks ───────────────────────────────────────────────────────────────

using ProgressCallback = std::function<void(int64_t downloaded, int64_t total)>;
using LogCallback      = std::function<void(LogLevel, const std::string&)>;

// ─── HTTP ────────────────────────────────────────────────────────────────────

struct HttpResponse {
    int         statusCode = 0;
    std::string body;
    std::map<std::string, std::string> headers;
    bool        error = false;
    std::string errorMsg;
};

struct HttpRequest {
    HttpMethod method = HttpMethod::GET;
    std::string url;
    std::map<std::string, std::string> headers;
    std::string body;
    int timeoutSec = 30;
    bool followRedirects = true;
};

// ─── GitHub API types ────────────────────────────────────────────────────────

struct GithubAsset {
    int         id = 0;
    std::string name;
    std::string contentType;
    int64_t     size = 0;
    std::string browserDownloadUrl;
    std::string apiDownloadUrl;
    std::string createdAt;
    int         downloadCount = 0;
    std::string label;
};

struct GithubRelease {
    int         id = 0;
    std::string tagName;
    std::string targetCommitish;
    std::string name;
    std::string body;
    bool        prerelease = false;
    bool        draft = false;
    std::string publishedAt;
    std::string createdAt;
    std::string htmlUrl;
    std::string tarballUrl;
    std::string zipballUrl;
    std::vector<GithubAsset> assets;
};

struct GithubRepo {
    std::string owner;
    std::string name;
    std::string fullName;
    std::string description;
    std::string defaultBranch;
    std::string htmlUrl;
    std::string apiUrl;
    std::string gitUrl;
    std::string sshUrl;
    std::string language;
    std::string license;
    std::string pushedAt;
    int         stars = 0;
    int         forks = 0;
    int         openIssues = 0;
    int         watchers = 0;
    bool        archived = false;
    bool        disabled = false;
    bool        fork = false;
    bool        hasIssues = false;
    bool        hasWiki = false;
};

struct GithubUser {
    std::string login;
    int         id = 0;
    std::string avatarUrl;
    std::string htmlUrl;
    std::string name;
    std::string email;
    std::string bio;
    int         publicRepos = 0;
    int         publicGists = 0;
    int         followers = 0;
    int         following = 0;
    std::string createdAt;
};

// ─── Version / Update ────────────────────────────────────────────────────────

struct Version {
    int major = 0;
    int minor = 0;
    int patch = 0;
    std::string prerelease; // e.g. "alpha.1", "rc.2"
    std::string buildMeta;  // e.g. "build.42"

    std::string toString() const;
    static Version fromString(const std::string& str);
    static int compare(const Version& a, const Version& b);
};

struct UpdateInfo {
    bool        available = false;
    Version     localVersion;
    Version     remoteVersion;
    GithubRelease release;
    std::vector<GithubAsset> matchingAssets;
    bool        error = false;
    std::string errorMessage;
};

struct DownloadResult {
    bool        success = false;
    std::string filePath;
    std::string checksumSha256;
    int64_t     bytesDownloaded = 0;
    int64_t     totalBytes = 0;
    std::string errorMsg;
};

// ─── Config ──────────────────────────────────────────────────────────────────

struct GithubConfig {
    std::string owner          = "StormDevzz";
    std::string repo           = "RaveX";
    std::string token;
    std::string currentVersion = "1.0.0";
    ReleaseChannel channel     = ReleaseChannel::Stable;
    bool autoUpdate            = true;
    bool verifyChecksums       = true;
    std::string downloadDir;
    std::string installDir;
    LogLevel logLevel          = LogLevel::Info;
};

} // namespace github
} // namespace ravex
