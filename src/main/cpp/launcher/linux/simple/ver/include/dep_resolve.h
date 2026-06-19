#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct dep_resolve_result {
    bool success;
    std::string message;
    std::string data;
};

dep_resolve_result dep_resolve_execute(const std::string& input);
double dep_resolve_calculate(const std::string& input);

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
