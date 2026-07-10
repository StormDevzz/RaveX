#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct compat_check_result {
    bool success;
    std::string message;
    std::string data;
};

compat_check_result compat_check_execute(const std::string& input);
double compat_check_calculate(const std::string& input);

}
}
}
}
