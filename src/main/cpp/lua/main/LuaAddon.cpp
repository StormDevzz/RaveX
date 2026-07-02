











#include "LuaAddon.hpp"
#include "LuaBridge.hpp"
#include "LuaRegistry.hpp"
#include <AddonContext.hpp>
#include <AddonConfig.hpp>
#include <cstdio>
#include <fstream>



namespace ravex { namespace lua {
    lua_State* createLuaState();
    void closeLuaState(lua_State* L);
    bool loadLuaFile(lua_State* L, const std::string& path, std::string& err);
    bool loadLuaString(lua_State* L, const std::string& code, std::string& err);
    bool hasLuaFunction(lua_State* L, const std::string& name);
    void setupSandbox(lua_State* L, bool enable);
}}

namespace ravex {
namespace lua {

LuaAddon::~LuaAddon() {
    unload();
}

LuaLoadResult LuaAddon::loadFromFile(const std::string& filePath,
                                      ravex::addon::AddonContext* ctx,
                                      ravex::addon::AddonConfig* cfg) {
    LuaLoadResult result;
    result.filePath = filePath;

    
    
    std::ifstream testFile(filePath);
    if (!testFile) {
        result.errorMsg = "File not found: " + filePath;
        return result;
    }
    testFile.close();

    
    
    m_L = createLuaState();
    if (!m_L) {
        result.errorMsg = "Failed to create Lua state";
        return result;
    }

    
    
    setupSandbox(m_L, true);

    
    
    m_luaCtx.setContext(ctx);
    m_luaCtx.setConfig(cfg);
    m_luaCtx.registerInState(m_L);

    
    
    std::string loadError;
    if (!loadLuaFile(m_L, filePath, loadError)) {
        result.errorMsg = "Failed to load script: " + loadError;
        m_luaCtx.unregisterFromState(m_L);
        closeLuaState(m_L);
        m_L = nullptr;
        return result;
    }

    
    
    m_meta.name        = readGlobalString("getName", "UnnamedLuaAddon");
    m_meta.version     = readGlobalString("getVersion", "0.0.0");
    m_meta.author      = readGlobalString("getAuthor", "Unknown");
    m_meta.description = readGlobalString("getDescription", "");

    
    
    m_hasTick  = hasLuaFunction(m_L, "onTick");
    m_hasEvent = hasLuaFunction(m_L, "onEvent");

    m_scriptPath = filePath;

    result.success = true;
    return result;
}

void LuaAddon::unload() {
    if (!m_L) return;

    if (m_running) {
        callOnUnload();
    }

    m_luaCtx.unregisterFromState(m_L);
    closeLuaState(m_L);
    m_L = nullptr;
    m_running = false;
    m_hasTick = false;
    m_hasEvent = false;
}

bool LuaAddon::callOnLoad() {
    if (!m_L) return false;
    if (!hasLuaFunction(m_L, "onLoad")) {
        m_running = true;
        return true;
    }
    bool ok = LuaBridge::callFunction(m_L, "onLoad", 0, 0);
    if (ok) m_running = true;
    return ok;
}

void LuaAddon::callOnUnload() {
    if (!m_L || !m_running) return;
    if (hasLuaFunction(m_L, "onUnload")) {
        LuaBridge::callFunction(m_L, "onUnload", 0, 0);
    }
    m_running = false;
}

void LuaAddon::callOnTick() {
    if (!m_L || !m_running || !m_hasTick) return;
    LuaBridge::callFunction(m_L, "onTick", 0, 0);
}

void LuaAddon::callOnEvent(const std::string& eventName) {
    if (!m_L || !m_running || !m_hasEvent) return;
    lua_getglobal(m_L, "onEvent");
    if (!lua_isfunction(m_L, -1)) {
        lua_pop(m_L, 1);
        return;
    }
    lua_pushstring(m_L, eventName.c_str());
    LuaBridge::callRef(m_L, 1, 0);
}

std::string LuaAddon::readGlobalString(const std::string& name,
                                        const std::string& defaultValue) {
    if (!m_L) return defaultValue;
    lua_getglobal(m_L, name.c_str());
    if (lua_isfunction(m_L, -1)) {
        
        
        if (lua_pcall(m_L, 0, 1, 0) == LUA_OK) {
            std::string val = LuaBridge::popString(m_L, -1);
            lua_pop(m_L, 1);
            return val.empty() ? defaultValue : val;
        }
        lua_pop(m_L, 1);
        return defaultValue;
    }
    std::string val = LuaBridge::popString(m_L, -1);
    lua_pop(m_L, 1);
    return val.empty() ? defaultValue : val;
}

bool LuaAddon::hasGlobalFunction(const std::string& name) {
    return hasLuaFunction(m_L, name);
}

} 
} 
