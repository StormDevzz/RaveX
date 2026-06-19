#include "include/forge_api.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool isForgeAvailable(const std::string& mcVersion) {
    (void)mcVersion;
    return false;
}

std::string getLatestForgeVersion(const std::string& mcVersion) {
    (void)mcVersion;
    return "";
}

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex
