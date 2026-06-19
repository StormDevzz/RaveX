#include "include/sys_info.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

sys_info_t get_sys_info() {
    sys_info_t result;
    result.available = true;
    result.label = "sys_info";
    result.value = "ok";
    return result;
}

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
