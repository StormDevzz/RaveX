#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct proc_info_t {
    bool available;
    std::string label;
    std::string value;
};

proc_info_t get_proc_info();

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
