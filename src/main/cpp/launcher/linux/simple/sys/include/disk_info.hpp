#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct disk_info_t {
    bool available;
    std::string label;
    std::string value;
};

disk_info_t get_disk_info();

} 
} 
} 
} 
