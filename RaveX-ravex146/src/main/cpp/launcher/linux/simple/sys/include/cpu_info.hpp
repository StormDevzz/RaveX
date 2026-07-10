#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct cpu_info_t {
    bool available;
    std::string label;
    std::string value;
};

cpu_info_t get_cpu_info();

}
}
}
}
