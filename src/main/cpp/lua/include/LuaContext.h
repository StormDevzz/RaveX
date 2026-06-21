#pragma once

#include <lua.hpp>
#include <string>
#include <memory>

namespace ravex {
namespace addon {
    class AddonContext;
    class AddonConfig;
}

namespace lua {

// RU: LuaContext — обёртка над AddonContext, доступная из Lua-скриптов.
//     Регистрирует в Lua-состоянии функции: logInfo, logWarn, logError,
//     configGet, configSet, getAddonDir, getPlatform и другие.
//
//     Каждый Lua-аддон получает свой экземпляр LuaContext при загрузке.
//
// EN: LuaContext — wrapper over AddonContext accessible from Lua scripts.
//     Registers functions in the Lua state: logInfo, logWarn, logError,
//     configGet, configSet, getAddonDir, getPlatform and others.
//
//     Each Lua addon gets its own LuaContext instance on load.
class LuaContext {
private:
    // RU: Контекст RaveX, через который аддон общается с ядром.
    // EN: RaveX context through which the addon communicates with the core.
    ravex::addon::AddonContext* m_ctx = nullptr;

    // RU: Конфиг аддона (ключ-значение).
    // EN: Addon config (key-value).
    ravex::addon::AddonConfig* m_config = nullptr;

    // RU: Указатель на lua_State, в который регистрируются функции.
    // EN: Pointer to the lua_State where functions are registered.
    lua_State* m_L = nullptr;

    // RU: Зарегистрированы ли уже функции в данном Lua-состоянии.
    // EN: Whether functions are already registered in this Lua state.
    bool m_registered = false;

public:
    LuaContext() = default;
    ~LuaContext() = default;

    // RU: Устанавливает контекст RaveX для этого LuaContext.
    // EN: Sets the RaveX context for this LuaContext.
    void setContext(ravex::addon::AddonContext* ctx) { m_ctx = ctx; }

    // RU: Устанавливает конфиг аддона.
    // EN: Sets the addon config.
    void setConfig(ravex::addon::AddonConfig* cfg) { m_config = cfg; }

    // RU: Регистрирует все RaveX-функции в данном Lua-состоянии.
    //     Вызывается один раз при инициализации аддона.
    //     Функции регистрируются в глобальной таблице _G.
    // EN: Registers all RaveX functions in the given Lua state.
    //     Called once during addon initialization.
    //     Functions are registered in the global table _G.
    void registerInState(lua_State* L);

    // RU: Снимает регистрацию (очищает глобальные функции).
    // EN: Unregisters (clears global functions).
    void unregisterFromState(lua_State* L);

private:
    // RU: Статические C-функции, вызываемые из Lua.
    //     Каждая соответствует одному из методов RaveX API.
    // EN: Static C functions callable from Lua.
    //     Each corresponds to one RaveX API method.
    static int lua_logInfo(lua_State* L);
    static int lua_logWarn(lua_State* L);
    static int lua_logError(lua_State* L);
    static int lua_configGet(lua_State* L);
    static int lua_configSet(lua_State* L);
    static int lua_getAddonDir(lua_State* L);
    static int lua_getPlatform(lua_State* L);
    static int lua_getApiVersion(lua_State* L);
};

} // namespace lua
} // namespace ravex
