#pragma once

#include "LuaTypes.h"
#include <lua.hpp>
#include <string>
#include <stdexcept>

namespace ravex {
namespace lua {

// RU: Класс-утилита для безопасной конвертации типов между C++ и Lua.
//     Содержит только статические методы. Не требует создания экземпляра.
// EN: Utility class for safe type conversion between C++ and Lua.
//     Contains only static methods. No instance needed.
class LuaBridge {
public:
    // RU: Проверяет тип значения на вершине стека Lua.
    // EN: Checks the type of the value at the top of the Lua stack.
    static LuaValueType getType(lua_State* L, int index);

    // RU: Безопасно получает строку из стека Lua с проверкой типа.
    //     Возвращает пустую строку при несоответствии типа.
    // EN: Safely gets a string from the Lua stack with type check.
    //     Returns empty string on type mismatch.
    static std::string popString(lua_State* L, int index);

    // RU: Безопасно получает число из стека Lua с проверкой типа.
    // EN: Safely gets a number from the Lua stack with type check.
    static double popNumber(lua_State* L, int index);

    // RU: Безопасно получает булево значение из стека Lua.
    // EN: Safely gets a boolean from the Lua stack.
    static bool popBoolean(lua_State* L, int index);

    // RU: Получает поле из таблицы Lua по ключу (строковому).
    //     Возвращает true, если поле существует.
    // EN: Gets a field from a Lua table by string key.
    //     Returns true if the field exists.
    static bool getTableField(lua_State* L, int tableIndex,
                              const std::string& key);

    // RU: Читает строковое поле из таблицы. Если поля нет — возвращает defaultValue.
    // EN: Reads a string field from a table. Returns defaultValue if missing.
    static std::string readStringField(lua_State* L, int tableIndex,
                                       const std::string& key,
                                       const std::string& defaultValue = "");

    // RU: Читает числовое поле из таблицы.
    // EN: Reads a numeric field from a table.
    static double readNumberField(lua_State* L, int tableIndex,
                                  const std::string& key,
                                  double defaultValue = 0.0);

    // RU: Вызывает функцию Lua по имени с защитой (pcall).
    //     args — количество аргументов на стеке.
    //     nresults — ожидаемое количество результатов.
    //     Возвращает true, если вызов успешен.
    // EN: Calls a Lua function by name with protection (pcall).
    //     args — number of arguments on the stack.
    //     nresults — expected number of results.
    //     Returns true on success.
    static bool callFunction(lua_State* L, const std::string& name,
                             int args = 0, int nresults = 0);

    // RU: Вызывает функцию Lua по ссылке (в стеке) с защитой.
    // EN: Calls a Lua function by reference (on stack) with protection.
    static bool callRef(lua_State* L, int nargs = 0, int nresults = 0);

    // RU: Регистрирует C-функцию как глобальную в Lua.
    // EN: Registers a C function as a global in Lua.
    static void registerFunction(lua_State* L, const std::string& name,
                                 lua_CFunction func);

    // RU: Проверяет, есть ли на стеке ошибка, и возвращает её текст.
    // EN: Checks if there is an error on the stack and returns its text.
    static std::string checkError(lua_State* L, int result);
};

} // namespace lua
} // namespace ravex
