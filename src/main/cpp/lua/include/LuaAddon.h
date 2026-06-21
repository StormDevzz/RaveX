#pragma once

#include "LuaTypes.h"
#include "LuaContext.h"
#include <lua.hpp>
#include <string>
#include <memory>

namespace ravex {
namespace addon {
    class Addon;
    class AddonContext;
    class AddonConfig;
}

namespace lua {

// RU: LuaAddon — обёртка Lua-скрипта в интерфейс Addon.
//     Каждый экземпляр владеет собственным lua_State и вызывает
//     Lua-функции onLoad, onUnload, getName, getVersion из скрипта.
//
//     Скрипт должен определить как минимум:
//       function getName()      -> string
//       function getVersion()   -> string
//       function onLoad(ctx)    -> nil
//       function onUnload()     -> nil
//
//     Опционально:
//       function getAuthor()    -> string
//       function getDescription() -> string
//       function onEvent(name)  -> nil (для событий)
//       function onTick()       -> nil (для тиков)
//
// EN: LuaAddon — wraps a Lua script into the Addon interface.
//     Each instance owns its own lua_State and calls Lua functions
//     onLoad, onUnload, getName, getVersion from the script.
//
//     The script must define at least:
//       function getName()      -> string
//       function getVersion()   -> string
//       function onLoad(ctx)    -> nil
//       function onUnload()     -> nil
//
//     Optional:
//       function getAuthor()    -> string
//       function getDescription() -> string
//       function onEvent(name)  -> nil (for events)
//       function onTick()       -> nil (for ticks)
class LuaAddon {
private:
    // RU: Указатель на lua_State, принадлежащий этому аддону.
    // EN: Pointer to the lua_State owned by this addon.
    lua_State* m_L = nullptr;

    // RU: Мета-информация об аддоне (из скрипта).
    // EN: Addon meta information (from script).
    LuaAddonMeta m_meta;

    // RU: Контекст RaveX (логирование, конфиг, события).
    // EN: RaveX context (logging, config, events).
    LuaContext m_luaCtx;

    // RU: Путь к файлу скрипта.
    // EN: Path to the script file.
    std::string m_scriptPath;

    // RU: Выполняется ли сейчас аддон (между onLoad и onUnload).
    // EN: Whether the addon is currently running (between onLoad and onUnload).
    bool m_running = false;

    // RU: Есть ли в скрипте функция onTick.
    // EN: Whether the script has an onTick function.
    bool m_hasTick = false;

    // RU: Есть ли в скрипте функция onEvent.
    // EN: Whether the script has an onEvent function.
    bool m_hasEvent = false;

public:
    LuaAddon() = default;
    ~LuaAddon();

    // RU: Запрещаем копирование (lua_State не копируется).
    // EN: Disable copying (lua_State is not copyable).
    LuaAddon(const LuaAddon&) = delete;
    LuaAddon& operator=(const LuaAddon&) = delete;

    // RU: Загружает Lua-скрипт из файла и инициализирует аддон.
    //     Создаёт lua_State, открывает стандартные библиотеки,
    //     регистрирует RaveX API, загружает и выполняет скрипт.
    //     После загрузки читает мета-информацию (getName/getVersion).
    //     Возвращает false при ошибке (текст в result.errorMsg).
    // EN: Loads a Lua script from file and initializes the addon.
    //     Creates lua_State, opens standard libraries,
    //     registers RaveX API, loads and runs the script.
    //     After loading reads meta info (getName/getVersion).
    //     Returns false on error (text in result.errorMsg).
    LuaLoadResult loadFromFile(const std::string& filePath,
                               ravex::addon::AddonContext* ctx,
                               ravex::addon::AddonConfig* cfg);

    // RU: Выгружает аддон: вызывает onUnload в Lua, закрывает lua_State.
    // EN: Unloads the addon: calls onUnload in Lua, closes lua_State.
    void unload();

    // RU: Вызывает onLoad(ctx) в Lua-скрипте.
    // EN: Calls onLoad(ctx) in the Lua script.
    bool callOnLoad();

    // RU: Вызывает onUnload() в Lua-скрипте.
    // EN: Calls onUnload() in the Lua script.
    void callOnUnload();

    // RU: Вызывает onTick() в Lua-скрипте (если определена).
    // EN: Calls onTick() in the Lua script (if defined).
    void callOnTick();

    // RU: Вызывает onEvent(eventName) в Lua-скрипте (если определена).
    // EN: Calls onEvent(eventName) in the Lua script (if defined).
    void callOnEvent(const std::string& eventName);

    // RU: Возвращает мета-информацию аддона.
    // EN: Returns the addon meta information.
    const LuaAddonMeta& getMeta() const { return m_meta; }

    // RU: Возвращает lua_State (для низкоуровневых операций).
    // EN: Returns the lua_State (for low-level operations).
    lua_State* getState() const { return m_L; }

    // RU: Загружен ли аддон (имеет ли lua_State).
    // EN: Whether the addon is loaded (has a lua_State).
    bool isLoaded() const { return m_L != nullptr; }

    // RU: Выполняется ли аддон (между onLoad и onUnload).
    // EN: Whether the addon is running (between onLoad and onUnload).
    bool isRunning() const { return m_running; }

    // RU: Путь к файлу скрипта.
    // EN: Path to the script file.
    const std::string& getScriptPath() const { return m_scriptPath; }

private:
    // RU: Читает строковое поле из глобальной таблицы Lua.
    //     Используется после загрузки скрипта для чтения getName/getVersion.
    // EN: Reads a string field from the Lua global table.
    //     Used after script loading to read getName/getVersion.
    std::string readGlobalString(const std::string& name,
                                 const std::string& defaultValue = "");

    // RU: Проверяет, определена ли функция в глобальной области Lua.
    // EN: Checks whether a function is defined in the Lua global scope.
    bool hasGlobalFunction(const std::string& name);
};

} // namespace lua
} // namespace ravex
