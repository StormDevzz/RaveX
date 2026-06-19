#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct perf_info_t {
    bool available;
    std::string label;
    std::string value;
};

perf_info_t get_perf_info();

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
