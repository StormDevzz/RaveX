#pragma once
#include <string>
#include <vector>

namespace ravex {
namespace addon {

class SystemUtils {
public:
    static std::vector<std::string> listFiles(const std::string& dir, const std::string& extension);
    static bool fileExists(const std::string& path);
};

}
}
