#include "include/integr_core.h"
#include "include/fabric_installer.h"
#include "include/fabric_launcher.h"
#include "../../checks/fabric_check.h"
#include <sys/stat.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool setupIntegrations(const std::string& kickxDir, const std::string& mcVersion) {
    // if fabric not installed, install it
    if (needsFabricSetup(kickxDir)) {
        if (!installFabric(kickxDir, mcVersion))
            return false;
    }
    return true;
}

bool needsFabricSetup(const std::string& kickxDir) {
    return !checks::isFabricInstalled(kickxDir);
}

bool needsForgeSetup(const std::string& kickxDir) {
    (void)kickxDir;
    return false;
}

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex
