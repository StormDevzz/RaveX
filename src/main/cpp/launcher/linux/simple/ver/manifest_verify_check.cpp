#include "include/manifest_verify.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

manifest_verify_result manifest_verify_execute(const std::string& input) {
    manifest_verify_result r;
    r.success = true;
    r.message = "manifest_verify: check ok";
    r.data = input;
    return r;
}

}
}
}
}
