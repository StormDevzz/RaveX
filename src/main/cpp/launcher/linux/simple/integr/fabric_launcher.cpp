#include "include/fabric_launcher.h"
#include <sys/stat.h>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool prepareFabricLaunch(const std::string& kickxDir, const std::string& mcVersion, std::string& mainClass) {
    // check if fabric-loader is installed
    std::string loaderDir = kickxDir + "/libraries/net/fabricmc/fabric-loader";
    struct stat st;
    if (stat(loaderDir.c_str(), &st) != 0 || !S_ISDIR(st.st_mode))
        return false;

    mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient";
    return true;
}

bool detectFabricInClasspath(const std::string& classpath) {
    return classpath.find("fabric-loader") != std::string::npos;
}

std::string getFabricMainClass() {
    return "net.fabricmc.loader.impl.launch.knot.KnotClient";
}

} // namespace integr
} // namespace simple
} // namespace launcher
} // namespace ravex
