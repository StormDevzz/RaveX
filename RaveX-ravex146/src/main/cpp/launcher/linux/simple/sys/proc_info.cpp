#include "include/proc_info.hpp"

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

}
}
}
}
