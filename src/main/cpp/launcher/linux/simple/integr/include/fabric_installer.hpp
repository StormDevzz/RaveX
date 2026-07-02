#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool installFabric(const std::string& kickxDir, const std::string& mcVersion);
bool installFabricLoader(const std::string& kickxDir, const std::string& mcVersion, const std::string& loaderVersion);
bool downloadFabricDependencies(const std::string& kickxDir, const std::string& mcVersion, const std::string& loaderVersion);

} 
} 
} 
} 
