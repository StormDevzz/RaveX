#pragma once
#include <functional>
#include <string>
#include <vector>

namespace ravex::game {

struct ModpackMod {
    std::string slug;
    std::string downloadUrl;
    std::string filename;
};

bool importCurseForgeModpack(const std::wstring& zipPath, const std::wstring& destDir, std::string* error,
                             const std::function<void(const std::string&)>& status);

bool importModrinthModpack(const std::wstring& zipPath, const std::wstring& destDir, std::string* error,
                           const std::function<void(const std::string&)>& status);

}
