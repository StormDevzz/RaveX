#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool prepareFabricLaunch(const std::string& kickxDir, const std::string& mcVersion, std::string& mainClass);
bool detectFabricInClasspath(const std::string& classpath);
std::string getFabricMainClass();

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex
