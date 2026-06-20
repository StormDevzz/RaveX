#include "include/AddonContext.h"
#include "include/AddonLogger.h"

namespace ravex {
namespace addon {

AddonContext::AddonContext(const std::string& name) : addonName(name) {}

std::string AddonContext::getAddonName() const { return addonName; }

void AddonContext::logInfo(const std::string& msg) {
    AddonLogger::info(addonName, msg);
}

}
}
