#pragma once

#include "LuaAddon.hpp"
#include <string>
#include <unordered_map>
#include <memory>

namespace ravex {
namespace lua {














class LuaRegistry {
private:


    std::unordered_map<std::string, LuaAddon*> m_addonMap;



    std::vector<LuaAddon*> m_addonList;



    LuaRegistry() = default;

public:


    static LuaRegistry& getInstance();



    LuaRegistry(const LuaRegistry&) = delete;
    LuaRegistry& operator=(const LuaRegistry&) = delete;





    void registerAddon(LuaAddon* addon);



    void unregisterAddon(const std::string& name);



    LuaAddon* findAddon(const std::string& name) const;



    std::vector<LuaAddon*> getAllAddons() const;



    size_t count() const { return m_addonMap.size(); }



    void clear();



    void tickAll();



    void eventAll(const std::string& eventName);
};

}
}
