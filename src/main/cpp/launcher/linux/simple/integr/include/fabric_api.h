#pragma once
#include <string>
#include <vector>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

struct FabricLibrary {
    std::string name;
    std::string url;
    std::string dest;
};

struct FabricProfile {
    std::string loaderVersion;
    std::string mainClass;
    std::vector<FabricLibrary> libraries;
};

std::string getLatestFabricLoader(const std::string& mcVersion);
FabricProfile getFabricProfile(const std::string& mcVersion, const std::string& loaderVersion);
bool fetchFabricProfileJson(const std::string& mcVersion, const std::string& loaderVersion, std::string& outJson);

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex
