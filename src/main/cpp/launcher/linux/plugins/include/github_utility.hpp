#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace plugins {

struct ReleaseInfo {
    std::string tagName;
    std::string downloadUrl;
    std::string minecraftVersion;
    bool success;
};

class GithubUtility {
public:

    static ReleaseInfo getLatestRelease();


    static bool downloadFile(const std::string& url, const std::string& destPath);
};

}
}
}
