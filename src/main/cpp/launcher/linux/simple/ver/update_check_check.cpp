#include "include/update_check.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

update_check_result update_check_execute(const std::string& input) {
    update_check_result r;
    r.success = true;
    r.message = "update_check: check ok";
    r.data = input;
    return r;
}

} 
} 
} 
} 
