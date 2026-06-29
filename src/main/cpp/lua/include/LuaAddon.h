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
































class LuaAddon {
private:
    
    
    lua_State* m_L = nullptr;

    
    
    LuaAddonMeta m_meta;

    
    
    LuaContext m_luaCtx;

    
    
    std::string m_scriptPath;

    
    
    bool m_running = false;

    
    
    bool m_hasTick = false;

    
    
    bool m_hasEvent = false;

public:
    LuaAddon() = default;
    ~LuaAddon();

    
    
    LuaAddon(const LuaAddon&) = delete;
    LuaAddon& operator=(const LuaAddon&) = delete;

    
    
    
    
    
    
    
    
    
    
    LuaLoadResult loadFromFile(const std::string& filePath,
                               ravex::addon::AddonContext* ctx,
                               ravex::addon::AddonConfig* cfg);

    
    
    void unload();

    
    
    bool callOnLoad();

    
    
    void callOnUnload();

    
    
    void callOnTick();

    
    
    void callOnEvent(const std::string& eventName);

    
    
    const LuaAddonMeta& getMeta() const { return m_meta; }

    
    
    lua_State* getState() const { return m_L; }

    
    
    bool isLoaded() const { return m_L != nullptr; }

    
    
    bool isRunning() const { return m_running; }

    
    
    const std::string& getScriptPath() const { return m_scriptPath; }

private:
    
    
    
    
    std::string readGlobalString(const std::string& name,
                                 const std::string& defaultValue = "");

    
    
    bool hasGlobalFunction(const std::string& name);
};

} 
} 
