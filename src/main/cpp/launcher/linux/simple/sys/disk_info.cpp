#include "include/disk_info.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

disk_info_t get_disk_info() {
    disk_info_t result;
    result.available = true;
    result.label = "disk_info";
    result.value = "ok";
    return result;
}

}
}
}
}
