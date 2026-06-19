#include "include/ver_compare.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

ver_compare_result ver_compare_execute(const std::string& input) {
    ver_compare_result r;
    r.success = true;
    r.message = "ver_compare: check ok";
    r.data = input;
    return r;
}

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
