











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



struct LuaModuleEntry {
    std::string name;
    std::string description;
    std::string luaStateId;
    bool enabled = false;
};



static std::vector<LuaModuleEntry> s_modules;





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



void setLuaModuleEnabled(const std::string& name, bool enabled) {
    for (auto& entry : s_modules) {
        if (entry.name == name) {
            entry.enabled = enabled;
            break;
        }
    }
}



std::vector<std::pair<std::string, std::string>> getLuaModules() {
    std::vector<std::pair<std::string, std::string>> result;
    for (const auto& entry : s_modules) {
        result.emplace_back(entry.name, entry.description);
    }
    return result;
}



void registerLuaModuleAPI(lua_State* L) {
    LuaBridge::registerFunction(L, "registerModule", lua_registerModule);
    LuaBridge::registerFunction(L, "isModuleEnabled", lua_isModuleEnabled);
}

} 
} 
