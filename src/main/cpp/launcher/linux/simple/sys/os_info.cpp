#include "include/os_info.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

os_info_t get_os_info() {
    os_info_t result;
    result.available = true;
    result.label = "os_info";
    result.value = "ok";
    return result;
}

} 
} 
} 
} 
