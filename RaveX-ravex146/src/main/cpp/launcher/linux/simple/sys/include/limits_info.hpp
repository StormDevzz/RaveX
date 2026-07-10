#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct limits_info_t {
    bool available;
    std::string label;
    std::string value;
};

limits_info_t get_limits_info();

}
}
}
}
