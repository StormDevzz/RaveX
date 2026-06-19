#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct ver_validator_result {
    bool success;
    std::string message;
    std::string data;
};

ver_validator_result ver_validator_execute(const std::string& input);
double ver_validator_calculate(const std::string& input);

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
