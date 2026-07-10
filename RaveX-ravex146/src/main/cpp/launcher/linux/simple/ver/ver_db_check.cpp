#include "include/ver_db.hpp"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

ver_db_result ver_db_execute(const std::string& input) {
    ver_db_result r;
    r.success = true;
    r.message = "ver_db: check ok";
    r.data = input;
    return r;
}

}
}
}
}
