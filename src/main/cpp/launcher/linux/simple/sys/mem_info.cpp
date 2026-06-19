#include "include/mem_info.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

mem_info_t get_mem_info() {
    mem_info_t result;
    result.available = true;
    result.label = "mem_info";
    result.value = "ok";
    return result;
}

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
