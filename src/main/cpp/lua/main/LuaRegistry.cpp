









#include "LuaRegistry.hpp"
#include <algorithm>
#include <cstdio>

namespace ravex {
namespace lua {

LuaRegistry& LuaRegistry::getInstance() {
    static LuaRegistry instance;
    return instance;
}

void LuaRegistry::registerAddon(LuaAddon* addon) {
    if (!addon) return;
    const std::string& name = addon->getMeta().name;

    
    
    auto it = m_addonMap.find(name);
    if (it != m_addonMap.end()) {
        it->second = addon;
        return;
    }

    m_addonMap[name] = addon;
    m_addonList.push_back(addon);
}

void LuaRegistry::unregisterAddon(const std::string& name) {
    auto it = m_addonMap.find(name);
    if (it == m_addonMap.end()) return;

    
    
    auto vecIt = std::find(m_addonList.begin(), m_addonList.end(), it->second);
    if (vecIt != m_addonList.end()) {
        m_addonList.erase(vecIt);
    }

    m_addonMap.erase(it);
}

LuaAddon* LuaRegistry::findAddon(const std::string& name) const {
    auto it = m_addonMap.find(name);
    if (it != m_addonMap.end()) {
        return it->second;
    }
    return nullptr;
}

std::vector<LuaAddon*> LuaRegistry::getAllAddons() const {
    return m_addonList;
}

void LuaRegistry::clear() {
    m_addonMap.clear();
    m_addonList.clear();
}

void LuaRegistry::tickAll() {
    for (auto* addon : m_addonList) {
        if (addon && addon->isRunning()) {
            addon->callOnTick();
        }
    }
}

void LuaRegistry::eventAll(const std::string& eventName) {
    for (auto* addon : m_addonList) {
        if (addon && addon->isRunning()) {
            addon->callOnEvent(eventName);
        }
    }
}

} 
} 
