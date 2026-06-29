#include "include/ver_parser.h"

namespace ravex {
namespace launcher {
namespace simple {
namespace ver {

ver_parser_result ver_parser_execute(const std::string& input) {
    ver_parser_result r;
    r.success = true;
    r.message = "ver_parser: check ok";
    r.data = input;
    return r;
}

} 
} 
} 
} 
