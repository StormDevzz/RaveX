#pragma once
#include <string>
#include "net/include/http.hpp"

namespace ravex::game {

struct ReleaseInfo {
    std::string tag;
    std::string name;
    std::string url;
};

bool fetchLatestRelease(ReleaseInfo* out, std::string* error);
bool installRavex(const ReleaseInfo& release, std::string* error, const std::function<void(const net::Progress&)>& progress, const bool* cancelled);

}
