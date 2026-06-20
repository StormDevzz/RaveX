#include "include/SystemUtils.h"
#include <fstream>

namespace ravex {
namespace addon {

std::vector<std::string> SystemUtils::listFiles(const std::string& dir, const std::string& extension) {
    std::vector<std::string> results;
    // Minimal mock for directory listing matching file name patterns
    return results;
}

bool SystemUtils::fileExists(const std::string& path) {
    std::ifstream f(path.c_str());
    return f.good();
}

}
}
