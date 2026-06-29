









#include "LuaBridge.h"
#include "LuaTypes.h"
#include <lua.hpp>
#include <string>
#include <cstdio>
#include <fstream>
#include <sstream>
#include <vector>

namespace ravex {
namespace lua {





lua_State* createLuaState() {
    lua_State* L = luaL_newstate();
    if (!L) return nullptr;
    luaL_openlibs(L);
    return L;
}



void closeLuaState(lua_State* L) {
    if (L) {
        lua_close(L);
    }
}





bool loadLuaFile(lua_State* L, const std::string& filePath,
                 std::string& errorOut) {
    int result = luaL_loadfile(L, filePath.c_str());
    if (result != LUA_OK) {
        errorOut = LuaBridge::checkError(L, result);
        return false;
    }
    result = lua_pcall(L, 0, 0, 0);
    if (result != LUA_OK) {
        errorOut = LuaBridge::checkError(L, result);
        return false;
    }
    return true;
}





bool loadLuaString(lua_State* L, const std::string& code,
                   std::string& errorOut) {
    int result = luaL_loadstring(L, code.c_str());
    if (result != LUA_OK) {
        errorOut = LuaBridge::checkError(L, result);
        return false;
    }
    result = lua_pcall(L, 0, 0, 0);
    if (result != LUA_OK) {
        errorOut = LuaBridge::checkError(L, result);
        return false;
    }
    return true;
}



bool hasLuaFunction(lua_State* L, const std::string& name) {
    lua_getglobal(L, name.c_str());
    bool isFunc = lua_isfunction(L, -1);
    lua_pop(L, 1);
    return isFunc;
}









void setupSandbox(lua_State* L, bool enable) {
    if (!enable) return;

    static const char* blocked[] = {
        "dofile", "loadfile", "require", "module",
        "collectgarbage", nullptr
    };
    for (int i = 0; blocked[i] != nullptr; i++) {
        lua_pushnil(L);
        lua_setglobal(L, blocked[i]);
    }

    lua_getglobal(L, "os");
    if (lua_istable(L, -1)) {
        static const char* osBlocked[] = {
            "execute", "exit", "rename", "remove",
            "tmpname", nullptr
        };
        for (int i = 0; osBlocked[i] != nullptr; i++) {
            lua_pushnil(L);
            lua_setfield(L, -2, osBlocked[i]);
        }
    }
    lua_pop(L, 1);

    lua_pushnil(L);
    lua_setglobal(L, "io");
}

} 
} 
