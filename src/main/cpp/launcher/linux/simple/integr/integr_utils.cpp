#include "include/integr_utils.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace integr {

std::string mavenGroupToPath(const std::string& group) {
    std::string path = group;
    for (char& ch : path)
        if (ch == '.') ch = '/';
    return path;
}

std::string mavenToPath(const std::string& mavenCoord) {
    
    size_t c1 = mavenCoord.find(':');
    if (c1 == std::string::npos) return "";

    size_t c2 = mavenCoord.find(':', c1 + 1);
    if (c2 == std::string::npos) return "";

    std::string group = mavenCoord.substr(0, c1);
    std::string artifact = mavenCoord.substr(c1 + 1, c2 - c1 - 1);
    std::string version = mavenCoord.substr(c2 + 1);

    std::string classifier;
    size_t at = version.find('@');
    if (at != std::string::npos) {
        classifier = version.substr(at + 1);
        version = version.substr(0, at);
    }

    std::string groupPath = mavenGroupToPath(group);
    std::string jarName = artifact + "-" + version;
    if (!classifier.empty())
        jarName += "-" + classifier;
    jarName += ".jar";

    return groupPath + "/" + artifact + "/" + version + "/" + jarName;
}

std::string mavenToUrl(const std::string& mavenCoord, const std::string& repoBase) {
    std::string path = mavenToPath(mavenCoord);
    if (path.empty()) return "";
    return repoBase + path;
}

} 
} 
} 
} 
