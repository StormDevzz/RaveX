#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool isForgeAvailable(const std::string& mcVersion);
std::string getLatestForgeVersion(const std::string& mcVersion);

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex
