#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct ver_compare_result {
    bool success;
    std::string message;
    std::string data;
};

ver_compare_result ver_compare_execute(const std::string& input);
double ver_compare_calculate(const std::string& input);

} 
} 
} 
} 
