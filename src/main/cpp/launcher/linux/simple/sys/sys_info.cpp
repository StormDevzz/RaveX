#include "include/sys_info.hpp"

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

}
}
}
}
