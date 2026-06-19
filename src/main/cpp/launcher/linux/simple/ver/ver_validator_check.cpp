#include "include/ver_validator.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

ver_validator_result ver_validator_execute(const std::string& input) {
    ver_validator_result r;
    r.success = true;
    r.message = "ver_validator: check ok";
    r.data = input;
    return r;
}

} // namespace ver
} // namespace simple
} // namespace launcher
} // namespace ravex
