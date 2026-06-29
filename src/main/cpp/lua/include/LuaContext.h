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












class LuaContext {
private:
    
    
    ravex::addon::AddonContext* m_ctx = nullptr;

    
    
    ravex::addon::AddonConfig* m_config = nullptr;

    
    
    lua_State* m_L = nullptr;

    
    
    bool m_registered = false;

public:
    LuaContext() = default;
    ~LuaContext() = default;

    
    
    void setContext(ravex::addon::AddonContext* ctx) { m_ctx = ctx; }

    
    
    void setConfig(ravex::addon::AddonConfig* cfg) { m_config = cfg; }

    
    
    
    
    
    
    void registerInState(lua_State* L);

    
    
    void unregisterFromState(lua_State* L);

private:
    
    
    
    
    static int lua_logInfo(lua_State* L);
    static int lua_logWarn(lua_State* L);
    static int lua_logError(lua_State* L);
    static int lua_configGet(lua_State* L);
    static int lua_configSet(lua_State* L);
    static int lua_getAddonDir(lua_State* L);
    static int lua_getPlatform(lua_State* L);
    static int lua_getApiVersion(lua_State* L);
};

} 
} 
