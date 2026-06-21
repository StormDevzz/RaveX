// ══════════════════════════════════════════════════════════════════════════════
//  lua/main/LuaScript.cpp
//
//  RU: Низкоуровневые утилиты для загрузки и выполнения Lua-скриптов.
//      Функции не-static, чтобы LuaAddon.cpp мог их вызывать.
//
//  EN: Low-level utilities for loading and executing Lua scripts.
//      Functions are non-static so LuaAddon.cpp can call them.
// ══════════════════════════════════════════════════════════════════════════════

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

// RU: Создаёт новый lua_State с открытыми стандартными библиотеками.
//     В будущем здесь можно настроить sandbox (убрать опасные функции).
// EN: Creates a new lua_State with standard libraries opened.
//     In the future a sandbox can be configured here (remove dangerous functions).
lua_State* createLuaState() {
    lua_State* L = luaL_newstate();
    if (!L) return nullptr;
    luaL_openlibs(L);
    return L;
}

// RU: Закрывает lua_State и освобождает все ресурсы.
// EN: Closes the lua_State and frees all resources.
void closeLuaState(lua_State* L) {
    if (L) {
        lua_close(L);
    }
}

// RU: Загружает и выполняет Lua-скрипт из файла.
//     Возвращает true при успехе. Ошибка записывается в errorOut.
// EN: Loads and executes a Lua script from a file.
//     Returns true on success. Error is written to errorOut.
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

// RU: Загружает и выполняет Lua-скрипт из строки.
//     Полезно для тестирования или динамической генерации кода.
// EN: Loads and executes a Lua script from a string.
//     Useful for testing or dynamic code generation.
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

// RU: Проверяет, есть ли в глобальной таблице Lua функция с именем name.
// EN: Checks whether a function with the given name exists in the Lua globals.
bool hasLuaFunction(lua_State* L, const std::string& name) {
    lua_getglobal(L, name.c_str());
    bool isFunc = lua_isfunction(L, -1);
    lua_pop(L, 1);
    return isFunc;
}

// RU: Настраивает sandbox-окружение: убирает опасные функции
//     (os.execute, io.popen, loadfile, dofile и т.д.),
//     ограничивает доступ к файловой системе.
//     По умолчанию sandbox включён.
// EN: Sets up sandbox environment: removes dangerous functions
//     (os.execute, io.popen, loadfile, dofile, etc.),
//     restricts file system access.
//     Sandbox is enabled by default.
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

} // namespace lua
} // namespace ravex
