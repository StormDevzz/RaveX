#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct ver_cache_result {
    bool success;
    std::string message;
    std::string data;
};

ver_cache_result ver_cache_execute(const std::string& input);
double ver_cache_calculate(const std::string& input);

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
