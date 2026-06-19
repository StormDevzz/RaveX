#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct manifest_verify_result {
    bool success;
    std::string message;
    std::string data;
};

manifest_verify_result manifest_verify_execute(const std::string& input);
double manifest_verify_calculate(const std::string& input);

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
