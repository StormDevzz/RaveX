#include "fabric_check.h"
#include <sys/stat.h>
#include <fstream>
#include <vector>
#include <cstdio>

namespace ravex {
namespace launcher {
namespace checks {

bool isFabricInstalled(const std::string& kickxDir) {
    std::string libsDir = kickxDir + "/libraries/net/fabricmc/fabric-loader";
    struct stat st;
    if (stat(libsDir.c_str(), &st) != 0 || !S_ISDIR(st.st_mode))
        return false;

    // check for at least one fabric-loader jar
    std::string findCmd = "find \"" + libsDir + "\" -name \"fabric-loader-*.jar\" 2>/dev/null | head -1";
    FILE* pipe = popen(findCmd.c_str(), "r");
    if (!pipe) return false;
    char buf[512];
    bool found = false;
    if (fgets(buf, sizeof(buf), pipe)) {
        found = (buf[0] != '\0');
    }
    pclose(pipe);
    return found;
}

std::string getFabricLoaderVersion(const std::string& kickxDir) {
    std::string libsDir = kickxDir + "/libraries/net/fabricmc/fabric-loader";
    std::string findCmd = "find \"" + libsDir + "\" -name \"fabric-loader-*.jar\" 2>/dev/null | head -1";
    FILE* pipe = popen(findCmd.c_str(), "r");
    if (!pipe) return "";
    char buf[512];
    std::string result;
    if (fgets(buf, sizeof(buf), pipe)) {
        result = buf;
        // trim newline
        if (!result.empty() && result.back() == '\n')
            result.pop_back();
        // extract version from path
        size_t slash = result.find_last_of('/');
        if (slash != std::string::npos) {
            result = result.substr(slash + 1);
        }
        // fabric-loader-X.Y.Z.jar → X.Y.Z
        size_t dash = result.find("fabric-loader-");
        if (dash != std::string::npos) {
            result = result.substr(dash + 14);
            size_t dot = result.rfind(".jar");
            if (dot != std::string::npos)
                result = result.substr(0, dot);
        }
    }
    pclose(pipe);
    return result;
}

bool hasFabricLibraries(const std::string& kickxDir) {
    if (!isFabricInstalled(kickxDir)) return false;

    // check for essential fabric dependencies
    std::vector<std::string> requiredPaths = {
        kickxDir + "/libraries/net/fabricmc/fabric-loader",
        kickxDir + "/libraries/org/ow2/asm",
    };

    for (const auto& path : requiredPaths) {
        struct stat st;
        if (stat(path.c_str(), &st) != 0 || !S_ISDIR(st.st_mode))
            return false;
    }
    return true;
}

} // namespace checks
} // namespace launcher
} // namespace ravex
