#pragma once
#include <string>

namespace ravex {
namespace addon {

class AddonLogger {
public:
    static void info(const std::string& prefix, const std::string& msg);
    static void warn(const std::string& prefix, const std::string& msg);
    static void error(const std::string& prefix, const std::string& msg);
};

}
}
