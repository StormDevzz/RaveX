#include "include/ver_base.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

ver_base_result ver_base_execute(const std::string& input) {
    ver_base_result r;
    r.success = true;
    r.message = "ver_base: check ok";
    r.data = input;
    return r;
}

}
}
}
}
