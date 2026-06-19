#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct env_info_t {
    bool available;
    std::string label;
    std::string value;
};

env_info_t get_env_info();

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
