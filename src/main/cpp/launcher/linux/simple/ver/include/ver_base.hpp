#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct ver_base_result {
    bool success;
    std::string message;
    std::string data;
};

ver_base_result ver_base_execute(const std::string& input);
double ver_base_calculate(const std::string& input);

}
}
}
}
