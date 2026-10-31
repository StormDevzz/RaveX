#include "include/AddonVersion.hpp"

#include <cstdio>

namespace ravex {
namespace addon {

std::string AddonVersion::getApiVersion() {
    return "1.5";
}

bool AddonVersion::isCompatible(const std::string& version) {
    // addons built for an older client keep working,
    // only addons that need a newer api are rejected
    int major = 0;
    int minor = 0;
    if (std::sscanf(version.c_str(), "%d.%d", &major, &minor) < 1) return true;
    if (major != 1) return major < 1;
    return minor <= 5;
}

}
}
