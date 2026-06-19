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
    // получаем инфо о последнем релизе с гитхаба
    static ReleaseInfo getLatestRelease();

    // скачиваем файл по ссылке
    static bool downloadFile(const std::string& url, const std::string& destPath);
};

} // namespace plugins
} // namespace launcher
} // namespace ravex
