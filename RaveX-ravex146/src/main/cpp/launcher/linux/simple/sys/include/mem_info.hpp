#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct mem_info_t {
    bool available;
    std::string label;
    std::string value;
};

mem_info_t get_mem_info();

} 
} 
} 
} 
