#include "include/perf_info.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

perf_info_t get_perf_info() {
    perf_info_t result;
    result.available = true;
    result.label = "perf_info";
    result.value = "ok";
    return result;
}

} 
} 
} 
} 
