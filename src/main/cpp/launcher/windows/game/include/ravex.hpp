#pragma once
#include <string>
#include "net/include/http.hpp"

namespace ravex::game {

struct ReleaseInfo {
    std::string tag;
    std::string name;
    std::string url;
    std::string sha256;
};

// self-update info from the newest GitHub release
struct LauncherUpdate {
    std::string version;
    std::string setupUrl;
    std::string setupSha256;  // from SHA256SUMS.txt, empty if not published
};

bool fetchLatestRelease(ReleaseInfo* out, std::string* error);
bool installRavex(const ReleaseInfo& release, std::string* error, const std::function<void(const net::Progress&)>& progress, const bool* cancelled);

// reads version / setup / hashes from the latest release
bool fetchLauncherUpdate(LauncherUpdate* out, std::string* error);

// release notes for the "what's new" dialog
struct ChangelogData {
    std::string title;
    std::string url;
    std::string text;
};
bool fetchChangelog(ChangelogData* out, std::string* error);
// numeric version compare, "1.2.10" > "1.2.9"; separators skipped
bool versionGreater(const std::string& a, const std::string& b);

}
