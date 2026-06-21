#pragma once

#include <string>
#include <vector>
#include <functional>

namespace ravex {
namespace lua {

// RU: Мета-информация о Lua-аддоне: имя, автор, версия.
//     Заполняется из скрипта при загрузке (функции getName/getVersion/getAuthor).
// EN: Lua addon meta information: name, author, version.
//     Filled from the script on load (getName/getVersion/getAuthor functions).
struct LuaAddonMeta {
    std::string name;
    std::string author;
    std::string version;
    std::string description;
    int apiLevel = 1;
};

// RU: Информация о Lua-модуле, зарегистрированном аддоном.
// EN: Information about a Lua module registered by the addon.
struct LuaModuleInfo {
    std::string name;
    std::string description;
    bool enabled = false;
};

// RU: Результат загрузки Lua-скрипта.
// EN: Result of loading a Lua script.
struct LuaLoadResult {
    bool success = false;
    std::string errorMsg;
    std::string filePath;
};

// RU: Уровни логирования для Lua.
// EN: Log levels for Lua.
enum class LuaLogLevel {
    Debug,
    Info,
    Warn,
    Error
};

// RU: Типы значений, которые можно передавать между C++ и Lua.
// EN: Value types that can be passed between C++ and Lua.
enum class LuaValueType {
    Nil,
    Boolean,
    Number,
    String,
    Table,
    Function
};

} // namespace lua
} // namespace ravex
