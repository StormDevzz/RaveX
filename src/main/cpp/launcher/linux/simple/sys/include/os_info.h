#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct os_info_t {
    bool available;
    std::string label;
    std::string value;
};

os_info_t get_os_info();

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
