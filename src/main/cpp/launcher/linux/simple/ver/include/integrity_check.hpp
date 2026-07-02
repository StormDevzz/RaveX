#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct integrity_check_result {
    bool success;
    std::string message;
    std::string data;
};

integrity_check_result integrity_check_execute(const std::string& input);
double integrity_check_calculate(const std::string& input);

} 
} 
} 
} 
