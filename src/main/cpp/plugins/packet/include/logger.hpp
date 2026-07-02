#pragma once

#include "packet_types.hpp"
#include <string>
#include <iostream>

namespace packet {
namespace log {

inline void log(LogLevel lvl, const std::string& msg) {
    static const char* labels[] = {"DBG", "INF", "WRN", "ERR"};
    int idx = static_cast<int>(lvl);
    if (idx < 0) idx = 0;
    if (idx > 3) idx = 3;
    std::cout << "[" << labels[idx] << "] " << msg << std::endl;
}

inline void debug(const std::string& msg) { log(LogLevel::Debug, msg); }
inline void info(const std::string& msg) { log(LogLevel::Info, msg); }
inline void warn(const std::string& msg) { log(LogLevel::Warn, msg); }
inline void error(const std::string& msg) { log(LogLevel::Error, msg); }

} 
} 
