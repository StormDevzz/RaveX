#include "include/integr_core.hpp"
#include "include/fabric_installer.hpp"
#include "include/fabric_launcher.hpp"
#include "../../checks/include/fabric_check.hpp"
#include <sys/stat.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool setupIntegrations(const std::string& kickxDir, const std::string& mcVersion) {
    
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

} 
} 
} 
} 
