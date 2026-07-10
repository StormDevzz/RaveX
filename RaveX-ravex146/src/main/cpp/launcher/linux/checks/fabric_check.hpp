#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace checks {

bool isFabricInstalled(const std::string& kickxDir);
std::string getFabricLoaderVersion(const std::string& kickxDir);
bool hasFabricLibraries(const std::string& kickxDir);

}
}
}
