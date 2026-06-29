#include "include/limits_info.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

limits_info_t get_limits_info() {
    limits_info_t result;
    result.available = true;
    result.label = "limits_info";
    result.value = "ok";
    return result;
}

} 
} 
} 
} 
