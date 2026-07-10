













#include "LuaLoader.hpp"
#include "LuaRegistry.hpp"
#include "LuaAddon.hpp"
#include <AddonContext.hpp>
#include <string>
#include <vector>
#include <memory>
#include <cstdio>



namespace ravex { namespace lua {
    void dispatchLuaEvent(const std::string& eventName);
}}



namespace ravex { namespace lua {
    std::vector<std::pair<std::string, std::string>> getLuaModules();
}}
#include <cstdio>
#include <memory>

namespace ravex {
namespace lua {



static std::unique_ptr<LuaLoader> s_loader;











int initLuaAddons(ravex::addon::AddonContext* ctx) {
    if (s_loader) {
        if (ctx) {
            ctx->logInfo("[LuaAPI] Lua subsystem already initialized");
        }
        return (int)s_loader->count();
    }

    if (ctx) {
        ctx->logInfo("[LuaAPI] Initializing Lua addon subsystem...");
    }



    s_loader = std::make_unique<LuaLoader>("addons/lua");





    int count = s_loader->loadAll(ctx);

    if (ctx) {
        ctx->logInfo("[LuaAPI] Lua subsystem ready. "
                     + std::to_string(count) + " addon(s) loaded");
    }

    return count;
}





void shutdownLuaAddons() {
    if (s_loader) {
        s_loader->unloadAll();
        s_loader.reset();
    }
    LuaRegistry::getInstance().clear();
}





void tickLuaAddons() {
    LuaRegistry::getInstance().tickAll();
}





void eventLuaAddons(const std::string& eventName) {
    dispatchLuaEvent(eventName);
}



bool isLuaInitialized() {
    return s_loader != nullptr;
}



int getLuaAddonCount() {
    return s_loader ? (int)s_loader->count() : 0;
}







bool loadLuaAddonFile(const std::string& filePath,
                       ravex::addon::AddonContext* ctx) {
    if (!s_loader) {
        s_loader = std::make_unique<LuaLoader>("addons/lua");
    }
    return s_loader->loadFile(filePath, ctx);
}



bool unloadLuaAddon(const std::string& name) {
    auto* addon = LuaRegistry::getInstance().findAddon(name);
    if (!addon) return false;

    LuaRegistry::getInstance().unregisterAddon(name);
    addon->unload();
    return true;
}



std::vector<std::string> listLuaAddons() {
    std::vector<std::string> names;
    auto addons = LuaRegistry::getInstance().getAllAddons();
    for (auto* addon : addons) {
        if (addon) {
            names.push_back(addon->getMeta().name);
        }
    }
    return names;
}

}
}
