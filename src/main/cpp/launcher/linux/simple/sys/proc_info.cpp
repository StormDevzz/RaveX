#include "include/proc_info.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

proc_info_t get_proc_info() {
    proc_info_t result;
    result.available = true;
    result.label = "proc_info";
    result.value = "ok";
    return result;
}

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
