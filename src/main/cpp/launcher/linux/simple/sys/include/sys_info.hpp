#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct sys_info_t {
    bool available;
    std::string label;
    std::string value;
};

sys_info_t get_sys_info();

}
}
}
}
