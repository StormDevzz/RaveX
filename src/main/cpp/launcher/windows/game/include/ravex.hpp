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

// launcher self-update info taken from the newest GitHub release.
struct LauncherUpdate {
    std::string version;      // content of kickx-version.txt (e.g. "1.1.5")
    std::string setupUrl;     // browser_download_url of KickXSetup.exe
    std::string setupSha256;  // hash from SHA256SUMS.txt (empty if not published)
};

bool fetchLatestRelease(ReleaseInfo* out, std::string* error);
bool installRavex(const ReleaseInfo& release, std::string* error, const std::function<void(const net::Progress&)>& progress, const bool* cancelled);

// fetch the latest release and read kickx-version.txt / KickXSetup.exe / SHA256SUMS.txt from it.
bool fetchLauncherUpdate(LauncherUpdate* out, std::string* error);

// release notes of the newest releases for the "what's new" dialog.
struct ChangelogData {
    std::string title;  // name of the newest release
    std::string url;    // its html page on github
    std::string text;   // formatted notes of the last releases
};
bool fetchChangelog(ChangelogData* out, std::string* error);
// numeric "1.2.10" > "1.2.9" comparison; non-digit separators are skipped.
bool versionGreater(const std::string& a, const std::string& b);

}
