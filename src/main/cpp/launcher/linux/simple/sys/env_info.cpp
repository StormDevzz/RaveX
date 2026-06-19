#include "include/env_info.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

env_info_t get_env_info() {
    env_info_t result;
    result.available = true;
    result.label = "env_info";
    result.value = "ok";
    return result;
}

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
