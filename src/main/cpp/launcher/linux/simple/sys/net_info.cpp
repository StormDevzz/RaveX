#include "include/net_info.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

net_info_t get_net_info() {
    net_info_t result;
    result.available = true;
    result.label = "net_info";
    result.value = "ok";
    return result;
}

} // namespace sys
} // namespace simple
} // namespace launcher
} // namespace ravex
