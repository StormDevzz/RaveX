#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct ver_api_result {
    bool success;
    std::string message;
    std::string data;
};

ver_api_result ver_api_execute(const std::string& input);
double ver_api_calculate(const std::string& input);

} 
} 
} 
} 
