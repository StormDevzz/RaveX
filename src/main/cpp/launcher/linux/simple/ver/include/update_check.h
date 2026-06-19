#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct update_check_result {
    bool success;
    std::string message;
    std::string data;
};

update_check_result update_check_execute(const std::string& input);
double update_check_calculate(const std::string& input);

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
