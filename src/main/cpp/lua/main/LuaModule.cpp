// ══════════════════════════════════════════════════════════════════════════════
//  lua/main/LuaModule.cpp
//
//  RU: Система регистрации модулей из Lua.
//      Позволяет Lua-аддонам регистрировать собственные модули,
//      которые появляются в GUI RaveX как переключаемые функции.
//
//  EN: Module registration system from Lua.
//      Allows Lua addons to register their own modules,
//      which appear in the RaveX GUI as toggleable features.
// ══════════════════════════════════════════════════════════════════════════════

#include "LuaBridge.h"
#include <string>
#include <vector>
#include <map>
#include <utility>
#include <cstdio>
#include <cstdio>
#include <map>
#include <vector>

namespace ravex {
namespace lua {

// RU: Структура, хранящая информацию о зарегистрированном Lua-модуле.
// EN: Structure storing information about a registered Lua module.
struct LuaModuleEntry {
    std::string name;
    std::string description;
    std::string luaStateId;
    bool enabled = false;
};

// RU: Глобальный список зарегистрированных модулей.
// EN: Global list of registered modules.
static std::vector<LuaModuleEntry> s_modules;

// RU: Вызывается из Lua: registerModule("name", "description")
//     Регистрирует новый модуль, который появится в GUI.
// EN: Called from Lua: registerModule("name", "description")
//     Registers a new module that will appear in the GUI.
int lua_registerModule(lua_State* L) {
    std::string name = LuaBridge::popString(L, 1);
    std::string desc = LuaBridge::popString(L, 2);

    if (name.empty()) return 0;

    LuaModuleEntry entry;
    entry.name = name;
    entry.description = desc.empty() ? name : desc;
    s_modules.push_back(entry);

    return 0;
}

// RU: Вызывается из Lua: isModuleEnabled("name") -> bool
//     Проверяет, включён ли модуль.
// EN: Called from Lua: isModuleEnabled("name") -> bool
//     Checks whether the module is enabled.
int lua_isModuleEnabled(lua_State* L) {
    std::string name = LuaBridge::popString(L, 1);

    for (const auto& entry : s_modules) {
        if (entry.name == name) {
            lua_pushboolean(L, entry.enabled ? 1 : 0);
            return 1;
        }
    }

    lua_pushboolean(L, 0);
    return 1;
}

// RU: Вызывается из C++ для включения/выключения Lua-модуля.
// EN: Called from C++ to enable/disable a Lua module.
void setLuaModuleEnabled(const std::string& name, bool enabled) {
    for (auto& entry : s_modules) {
        if (entry.name == name) {
            entry.enabled = enabled;
            break;
        }
    }
}

// RU: Получает список всех зарегистрированных Lua-модулей.
// EN: Gets the list of all registered Lua modules.
std::vector<std::pair<std::string, std::string>> getLuaModules() {
    std::vector<std::pair<std::string, std::string>> result;
    for (const auto& entry : s_modules) {
        result.emplace_back(entry.name, entry.description);
    }
    return result;
}

// RU: Регистрирует функции API модулей в Lua-состоянии.
// EN: Registers module API functions in the Lua state.
void registerLuaModuleAPI(lua_State* L) {
    LuaBridge::registerFunction(L, "registerModule", lua_registerModule);
    LuaBridge::registerFunction(L, "isModuleEnabled", lua_isModuleEnabled);
}

} // namespace lua
} // namespace ravex
