#pragma once
#include <string>

namespace ravex {
namespace launcher {
namespace simple {
namespace sys {

struct net_info_t {
    bool available;
    std::string label;
    std::string value;
};

net_info_t get_net_info();

} 
} 
} 
} 
