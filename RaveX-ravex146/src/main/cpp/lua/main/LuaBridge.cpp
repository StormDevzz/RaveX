









#include "LuaBridge.hpp"
#include <cstdio>

namespace ravex {
namespace lua {

LuaValueType LuaBridge::getType(lua_State* L, int index) {
    int t = lua_type(L, index);
    switch (t) {
        case LUA_TNIL:          return LuaValueType::Nil;
        case LUA_TBOOLEAN:      return LuaValueType::Boolean;
        case LUA_TNUMBER:       return LuaValueType::Number;
        case LUA_TSTRING:       return LuaValueType::String;
        case LUA_TTABLE:        return LuaValueType::Table;
        case LUA_TFUNCTION:     return LuaValueType::Function;
        default:                return LuaValueType::Nil;
    }
}

std::string LuaBridge::popString(lua_State* L, int index) {
    if (lua_isstring(L, index)) {
        size_t len = 0;
        const char* str = lua_tolstring(L, index, &len);
        return std::string(str, len);
    }
    return {};
}

double LuaBridge::popNumber(lua_State* L, int index) {
    if (lua_isnumber(L, index)) {
        return (double)lua_tonumber(L, index);
    }
    return 0.0;
}

bool LuaBridge::popBoolean(lua_State* L, int index) {
    if (lua_isboolean(L, index)) {
        return lua_toboolean(L, index) != 0;
    }
    return false;
}

bool LuaBridge::getTableField(lua_State* L, int tableIndex,
                               const std::string& key) {




    if (tableIndex < 0) {
        tableIndex = lua_gettop(L) + tableIndex + 1;
    }
    lua_getfield(L, tableIndex, key.c_str());
    return !lua_isnil(L, -1);
}

std::string LuaBridge::readStringField(lua_State* L, int tableIndex,
                                        const std::string& key,
                                        const std::string& defaultValue) {
    if (getTableField(L, tableIndex, key)) {
        std::string val = popString(L, -1);
        lua_pop(L, 1);
        return val.empty() ? defaultValue : val;
    }
    lua_pop(L, 1);
    return defaultValue;
}

double LuaBridge::readNumberField(lua_State* L, int tableIndex,
                                   const std::string& key,
                                   double defaultValue) {
    if (getTableField(L, tableIndex, key)) {
        double val = popNumber(L, -1);
        lua_pop(L, 1);
        return val;
    }
    lua_pop(L, 1);
    return defaultValue;
}

bool LuaBridge::callFunction(lua_State* L, const std::string& name,
                              int args, int nresults) {
    lua_getglobal(L, name.c_str());
    if (!lua_isfunction(L, -1)) {
        lua_pop(L, args + 1);
        return false;
    }


    if (args > 0) {
        lua_insert(L, -args - 1);
    }
    int result = lua_pcall(L, args, nresults, 0);
    if (result != LUA_OK) {
        std::fprintf(stderr, "[LuaBridge] callFunction(%s) error: %s\n",
                     name.c_str(), lua_tostring(L, -1));
        lua_pop(L, 1);
        return false;
    }
    return true;
}

bool LuaBridge::callRef(lua_State* L, int nargs, int nresults) {
    int result = lua_pcall(L, nargs, nresults, 0);
    if (result != LUA_OK) {
        std::fprintf(stderr, "[LuaBridge] callRef error: %s\n",
                     lua_tostring(L, -1));
        lua_pop(L, 1);
        return false;
    }
    return true;
}

void LuaBridge::registerFunction(lua_State* L, const std::string& name,
                                  lua_CFunction func) {
    lua_pushcfunction(L, func);
    lua_setglobal(L, name.c_str());
}

std::string LuaBridge::checkError(lua_State* L, int result) {
    if (result != LUA_OK) {
        std::string err = lua_tostring(L, -1);
        lua_pop(L, 1);
        return err;
    }
    return {};
}

}
}
