#include "include/compat_check.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

compat_check_result compat_check_execute(const std::string& input) {
    compat_check_result r;
    r.success = true;
    r.message = "compat_check: check ok";
    r.data = input;
    return r;
}

} 
} 
} 
} 
