// ══════════════════════════════════════════════════════════════════════════════
//  lua/main/LuaTypes.cpp
//
//  RU: Реализация функций-утилит для работы с типами Lua.
//      Содержит вспомогательные методы для конвертации и проверки.
//
//  EN: Implementation of utility functions for Lua type handling.
//      Contains helper methods for conversion and validation.
// ══════════════════════════════════════════════════════════════════════════════

#include "LuaTypes.h"

namespace ravex {
namespace lua {

// RU: Преобразует LuaLogLevel в строку для логирования.
// EN: Converts LuaLogLevel to a string for logging.
const char* logLevelToString(LuaLogLevel level) {
    switch (level) {
        case LuaLogLevel::Debug: return "DEBUG";
        case LuaLogLevel::Info:  return "INFO";
        case LuaLogLevel::Warn:  return "WARN";
        case LuaLogLevel::Error: return "ERROR";
        default: return "UNKNOWN";
    }
}

} // namespace lua
} // namespace ravex
