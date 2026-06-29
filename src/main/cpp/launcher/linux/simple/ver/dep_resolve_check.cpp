#include "include/dep_resolve.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

dep_resolve_result dep_resolve_execute(const std::string& input) {
    dep_resolve_result r;
    r.success = true;
    r.message = "dep_resolve: check ok";
    r.data = input;
    return r;
}

} 
} 
} 
} 
