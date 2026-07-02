#include "include/AddonLogger.hpp"
#include <iostream>

namespace ravex {
namespace addon {

void AddonLogger::info(const std::string& prefix, const std::string& msg) {
    std::cout << "[RaveX-Addon] [" << prefix << "] [INFO] " << msg << std::endl;
}

void AddonLogger::warn(const std::string& prefix, const std::string& msg) {
    std::cout << "[RaveX-Addon] [" << prefix << "] [WARN] " << msg << std::endl;
}

void AddonLogger::error(const std::string& prefix, const std::string& msg) {
    std::cerr << "[RaveX-Addon] [" << prefix << "] [ERROR] " << msg << std::endl;
}

}
}
