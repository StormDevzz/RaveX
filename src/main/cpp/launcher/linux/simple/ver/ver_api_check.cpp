#include "include/ver_api.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

ver_api_result ver_api_execute(const std::string& input) {
    ver_api_result r;
    r.success = true;
    r.message = "ver_api: check ok";
    r.data = input;
    return r;
}

} 
} 
} 
} 
