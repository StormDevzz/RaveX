#pragma once
#include <string>
#include <vector>

namespace ravex::game {

struct ModUpdateInfo {
    std::string filename;
    std::string modSlug;
    std::string modTitle;
    std::string currentVersion;
    std::string latestVersion;
    std::string downloadUrl;
};

bool checkModUpdates(const std::wstring& modsDir, const std::string& mcVersion, std::vector<ModUpdateInfo>* out, std::string* error);

bool downloadModUpdate(const ModUpdateInfo& info, const std::wstring& modsDir, std::string* error);

}
