#include "include/integr_validator.hpp"
#include "include/integr_core.hpp"
#include "../../checks/fabric_check.hpp"
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
