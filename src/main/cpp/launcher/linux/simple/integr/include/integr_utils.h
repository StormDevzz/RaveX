#pragma once
#include <string>
#include <vector>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

std::string mavenToPath(const std::string& mavenCoord);
std::string mavenToUrl(const std::string& mavenCoord, const std::string& repoBase);
std::string mavenGroupToPath(const std::string& group);

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex
