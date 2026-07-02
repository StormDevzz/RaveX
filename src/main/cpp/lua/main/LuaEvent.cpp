













#include "LuaBridge.hpp"
#include "LuaRegistry.hpp"
#include <string>
#include <vector>
#include <map>
#include <cstdio>
#include <cstdio>
#include <map>

namespace ravex {
namespace lua {





static std::map<std::string, std::vector<std::string>> s_handlers;





int lua_registerEvent(lua_State* L) {
    std::string eventName = LuaBridge::popString(L, 1);
    std::string funcName = LuaBridge::popString(L, 2);

    if (eventName.empty() || funcName.empty()) {
        return 0;
    }

    s_handlers[eventName].push_back(funcName);
    return 0;
}







void dispatchLuaEvent(const std::string& eventName) {
    LuaRegistry::getInstance().eventAll(eventName);
}







void dispatchLuaEventWithData(const std::string& eventName,
                              const std::map<std::string, std::string>& data) {
    

    LuaRegistry::getInstance().eventAll(eventName);
}





void registerLuaEventAPI(lua_State* L) {
    LuaBridge::registerFunction(L, "registerEvent", lua_registerEvent);
}

} 
} 
