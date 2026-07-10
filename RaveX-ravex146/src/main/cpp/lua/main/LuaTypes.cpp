









#include "LuaTypes.hpp"

namespace ravex {
namespace lua {



const char* logLevelToString(LuaLogLevel level) {
    switch (level) {
        case LuaLogLevel::Debug: return "DEBUG";
        case LuaLogLevel::Info:  return "INFO";
        case LuaLogLevel::Warn:  return "WARN";
        case LuaLogLevel::Error: return "ERROR";
        default: return "UNKNOWN";
    }
}

}
}
