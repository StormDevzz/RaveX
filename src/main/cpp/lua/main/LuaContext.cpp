













#include "LuaContext.h"
#include "LuaBridge.h"
#include <AddonContext.h>
#include <AddonConfig.h>
#include <cstdio>
#include <string>

namespace ravex {
namespace lua {





static LuaContext* getCtxFromState(lua_State* L) {
    lua_getglobal(L, "__ravex_lua_ctx");
    if (lua_islightuserdata(L, -1)) {
        LuaContext* ctx = (LuaContext*)lua_touserdata(L, -1);
        lua_pop(L, 1);
        return ctx;
    }
    lua_pop(L, 1);
    return nullptr;
}





int LuaContext::lua_logInfo(lua_State* L) {
    auto* self = getCtxFromState(L);
    if (!self || !self->m_ctx) return 0;
    std::string msg = LuaBridge::popString(L, 1);
    self->m_ctx->logInfo("[Lua] " + msg);
    return 0;
}

int LuaContext::lua_logWarn(lua_State* L) {
    auto* self = getCtxFromState(L);
    if (!self || !self->m_ctx) return 0;
    std::string msg = LuaBridge::popString(L, 1);
    self->m_ctx->logInfo("[Lua WARN] " + msg);
    return 0;
}

int LuaContext::lua_logError(lua_State* L) {
    auto* self = getCtxFromState(L);
    if (!self || !self->m_ctx) return 0;
    std::string msg = LuaBridge::popString(L, 1);
    self->m_ctx->logInfo("[Lua ERROR] " + msg);
    return 0;
}

int LuaContext::lua_configGet(lua_State* L) {
    auto* self = getCtxFromState(L);
    if (!self || !self->m_config) return 0;
    std::string key = LuaBridge::popString(L, 1);
    std::string def = LuaBridge::popString(L, 2);
    std::string val = self->m_config->get(key, def);
    lua_pushstring(L, val.c_str());
    return 1;
}

int LuaContext::lua_configSet(lua_State* L) {
    auto* self = getCtxFromState(L);
    if (!self || !self->m_config) return 0;
    std::string key = LuaBridge::popString(L, 1);
    std::string val = LuaBridge::popString(L, 2);
    self->m_config->set(key, val);
    return 0;
}

int LuaContext::lua_getAddonDir(lua_State* L) {
    lua_pushstring(L, "addons/lua");
    return 1;
}

int LuaContext::lua_getPlatform(lua_State* L) {
#ifdef _WIN32
    lua_pushstring(L, "windows");
#else
    lua_pushstring(L, "linux");
#endif
    return 1;
}

int LuaContext::lua_getApiVersion(lua_State* L) {
    lua_pushstring(L, "1.4.3");
    return 1;
}

void LuaContext::registerInState(lua_State* L) {
    if (!L || m_registered) return;
    m_L = L;

    
    
    lua_pushlightuserdata(L, this);
    lua_setglobal(L, "__ravex_lua_ctx");

    
    
    LuaBridge::registerFunction(L, "logInfo",      lua_logInfo);
    LuaBridge::registerFunction(L, "logWarn",      lua_logWarn);
    LuaBridge::registerFunction(L, "logError",     lua_logError);
    LuaBridge::registerFunction(L, "configGet",    lua_configGet);
    LuaBridge::registerFunction(L, "configSet",    lua_configSet);
    LuaBridge::registerFunction(L, "getAddonDir",  lua_getAddonDir);
    LuaBridge::registerFunction(L, "getPlatform",  lua_getPlatform);
    LuaBridge::registerFunction(L, "getApiVersion", lua_getApiVersion);

    m_registered = true;
}

void LuaContext::unregisterFromState(lua_State* L) {
    if (!L || !m_registered) return;

    
    
    static const char* funcs[] = {
        "logInfo", "logWarn", "logError",
        "configGet", "configSet",
        "getAddonDir", "getPlatform", "getApiVersion",
        "__ravex_lua_ctx", nullptr
    };
    for (int i = 0; funcs[i] != nullptr; i++) {
        lua_pushnil(L);
        lua_setglobal(L, funcs[i]);
    }

    m_registered = false;
    m_L = nullptr;
}

} 
} 
