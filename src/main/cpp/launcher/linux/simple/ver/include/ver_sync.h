#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

struct ver_sync_result {
    bool success;
    std::string message;
    std::string data;
};

ver_sync_result ver_sync_execute(const std::string& input);
double ver_sync_calculate(const std::string& input);

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
