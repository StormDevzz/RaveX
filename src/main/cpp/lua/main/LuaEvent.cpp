// ══════════════════════════════════════════════════════════════════════════════
//  lua/main/LuaEvent.cpp
//
//  RU: Система событий для Lua-аддонов. Позволяет:
//      - Регистрировать обработчики событий из Lua
//      - Отправлять события из C++ во все Lua-аддоны
//      - Передавать данные события в Lua (имя события + параметры)
//
//  EN: Event system for Lua addons. Allows:
//      - Registering event handlers from Lua
//      - Dispatching events from C++ to all Lua addons
//      - Passing event data to Lua (event name + parameters)
// ══════════════════════════════════════════════════════════════════════════════

#include "LuaBridge.h"
#include "LuaRegistry.h"
#include <string>
#include <vector>
#include <map>
#include <cstdio>
#include <cstdio>
#include <map>

namespace ravex {
namespace lua {

// RU: Глобальная карта зарегистрированных обработчиков событий.
//     Ключ — имя события, значение — список имён Lua-функций.
// EN: Global map of registered event handlers.
//     Key — event name, value — list of Lua function names.
static std::map<std::string, std::vector<std::string>> s_handlers;

// RU: Регистрирует Lua-функцию как обработчик события.
//     Вызывается из Lua: registerEvent("eventName", "callbackFunction")
// EN: Registers a Lua function as an event handler.
//     Called from Lua: registerEvent("eventName", "callbackFunction")
int lua_registerEvent(lua_State* L) {
    std::string eventName = LuaBridge::popString(L, 1);
    std::string funcName = LuaBridge::popString(L, 2);

    if (eventName.empty() || funcName.empty()) {
        return 0;
    }

    s_handlers[eventName].push_back(funcName);
    return 0;
}

// RU: Отправляет событие всем Lua-аддонам, у которых есть onEvent.
//     Вызывается из C++, когда происходит системное событие RaveX.
//     Все Lua-аддоны с функцией onEvent получают имя события.
// EN: Dispatches an event to all Lua addons that have onEvent.
//     Called from C++ when a RaveX system event occurs.
//     All Lua addons with an onEvent function receive the event name.
void dispatchLuaEvent(const std::string& eventName) {
    LuaRegistry::getInstance().eventAll(eventName);
}

// RU: Отправляет событие с дополнительными данными.
//     Параметры передаются в Lua как таблица.
//     Пока не реализовано — для расширения в будущем.
// EN: Dispatches an event with additional data.
//     Parameters are passed to Lua as a table.
//     Not yet implemented — for future extension.
void dispatchLuaEventWithData(const std::string& eventName,
                              const std::map<std::string, std::string>& data) {
    // RU: Отправляем всем аддонам, у которых есть onEvent.
// EN: Dispatch to all addons that have onEvent.
    LuaRegistry::getInstance().eventAll(eventName);
}

// RU: Регистрирует функцию registerEvent в Lua-состоянии.
//     Вызывается при инициализации Lua-аддона.
// EN: Registers the registerEvent function in the Lua state.
//     Called during Lua addon initialization.
void registerLuaEventAPI(lua_State* L) {
    LuaBridge::registerFunction(L, "registerEvent", lua_registerEvent);
}

} // namespace lua
} // namespace ravex
