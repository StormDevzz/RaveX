#include "include/AddonVersion.h"

namespace ravex {
namespace addon {

std::string AddonVersion::getApiVersion() {
    return "1.0.0";
}

bool AddonVersion::isCompatible(const std::string& version) {
    return version == "1.0.0";
}

}
}
