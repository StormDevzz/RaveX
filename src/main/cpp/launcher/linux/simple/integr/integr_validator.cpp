#include "include/integr_validator.h"
#include "include/integr_core.h"
#include "../../checks/fabric_check.h"
#include <sys/stat.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool validateIntegration(const std::string& kickxDir) {
    if (!checks::isFabricInstalled(kickxDir)) return false;
    if (!checks::hasFabricLibraries(kickxDir)) return false;
    return true;
}

bool checkLoaderJar(const std::string& path) {
    struct stat st;
    return (stat(path.c_str(), &st) == 0 && st.st_size > 0);
}

} 
} 
} 
} 
