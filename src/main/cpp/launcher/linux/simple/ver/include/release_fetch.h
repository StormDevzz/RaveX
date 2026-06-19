#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct release_fetch_result {
    bool success;
    std::string message;
    std::string data;
};

release_fetch_result release_fetch_execute(const std::string& input);
double release_fetch_calculate(const std::string& input);

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
