#include "include/cpu_info.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

cpu_info_t get_cpu_info() {
    cpu_info_t result;
    result.available = true;
    result.label = "cpu_info";
    result.value = "ok";
    return result;
}

}
}
}
}
