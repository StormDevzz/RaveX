#include "include/offline_auth.hpp"
#include <sstream>
#include <iomanip>
#include <functional>

namespace ravex {
namespace launcher {
namespace simple {
namespace acc {

std::string generate_offline_uuid(const std::string& username) {
    size_t hash = std::hash<std::string>{}(username);
    size_t hash2 = std::hash<std::string>{}(username + "kickx_salt");

    std::stringstream ss;
    ss << std::hex << std::setfill('0');
    ss << std::setw(8) << (hash & 0xFFFFFFFF) << "-";
    ss << std::setw(4) << ((hash >> 32) & 0xFFFF) << "-";
    ss << "3" << std::setw(3) << ((hash2 & 0x0FFF)) << "-";
    ss << std::setw(4) << (((hash2 >> 16) & 0x3FFF) | 0x8000) << "-";
    ss << std::setw(12) << (((hash2 >> 24) | (hash << 24)) & 0xFFFFFFFFFFFFull);

    return ss.str();
}

}
}
}
}
