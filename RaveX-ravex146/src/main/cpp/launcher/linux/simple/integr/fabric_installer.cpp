#include "include/fabric_installer.hpp"
#include "include/fabric_api.hpp"
#include "include/integr_utils.hpp"
#include "../../network/include/http_client.hpp"
#include <sys/stat.h>
#include <fstream>
#include <cstdlib>
#include <iostream>

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

bool installFabric(const std::string& kickxDir, const std::string& mcVersion) {
    std::string loaderVersion = getLatestFabricLoader(mcVersion);
    if (loaderVersion.empty()) {
        std::cerr << "[RaveX] Fabric: no loader version found for MC " << mcVersion << std::endl;
        return false;
    }
    std::cerr << "[RaveX] Fabric: installing loader " << loaderVersion << " for MC " << mcVersion << std::endl;
    return installFabricLoader(kickxDir, mcVersion, loaderVersion);
}

bool installFabricLoader(const std::string& kickxDir, const std::string& mcVersion, const std::string& loaderVersion) {
    if (!downloadFabricDependencies(kickxDir, mcVersion, loaderVersion)) {
        std::cerr << "[RaveX] Fabric: dependency download failed" << std::endl;
        return false;
    }


    std::string jarName = "fabric-loader-" + loaderVersion + ".jar";
    std::string dest = kickxDir + "/libraries/net/fabricmc/fabric-loader/" + loaderVersion + "/" + jarName;
    struct stat st;
    if (stat(dest.c_str(), &st) == 0) {
        std::cerr << "[RaveX] Fabric: loader jar already exists" << std::endl;
        return true;
    }

    std::string url = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/"
                      + loaderVersion + "/" + jarName;
    bool ok = network::http_download(url, dest);
    std::cerr << "[RaveX] Fabric: downloading loader jar: " << (ok ? "OK" : "FAILED") << std::endl;
    return ok;
}

bool downloadFabricDependencies(const std::string& kickxDir, const std::string& mcVersion, const std::string& loaderVersion) {
    FabricProfile profile = getFabricProfile(mcVersion, loaderVersion);
    if (profile.libraries.empty()) {
        std::cerr << "[RaveX] Fabric: profile has no libraries" << std::endl;
        return false;
    }

    int dlCount = 0;
    for (const auto& lib : profile.libraries) {
        std::string dest = kickxDir + "/libraries/" + mavenToPath(lib.name);
        struct stat st;
        if (stat(dest.c_str(), &st) == 0) continue;

        std::string url = lib.url + mavenToPath(lib.name);
        if (network::http_download(url, dest)) dlCount++;
    }


    {
        std::string interJar = "intermediary-" + mcVersion + "-v2.jar";
        std::string interDest = kickxDir + "/libraries/net/fabricmc/intermediary/"
                                + mcVersion + "/" + interJar;
        struct stat st;
        if (stat(interDest.c_str(), &st) != 0) {
            std::string interUrl = "https://maven.fabricmc.net/net/fabricmc/intermediary/"
                                   + mcVersion + "/" + interJar;
            if (network::http_download(interUrl, interDest)) dlCount++;
        }
    }

    std::cerr << "[RaveX] Fabric: downloaded " << dlCount << "/"
              << profile.libraries.size() << " libraries" << std::endl;
    return true;
}

}
}
}
}
