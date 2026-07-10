#pragma once

#include "LuaTypes.hpp"
#include <lua.h>
#include <string>
#include <stdexcept>

namespace ravex {
namespace lua {





class LuaBridge {
public:


    static LuaValueType getType(lua_State* L, int index);





    static std::string popString(lua_State* L, int index);



    static double popNumber(lua_State* L, int index);



    static bool popBoolean(lua_State* L, int index);





    static bool getTableField(lua_State* L, int tableIndex,
                              const std::string& key);



    static std::string readStringField(lua_State* L, int tableIndex,
                                       const std::string& key,
                                       const std::string& defaultValue = "");



    static double readNumberField(lua_State* L, int tableIndex,
                                  const std::string& key,
                                  double defaultValue = 0.0);









    static bool callFunction(lua_State* L, const std::string& name,
                             int args = 0, int nresults = 0);



    static bool callRef(lua_State* L, int nargs = 0, int nresults = 0);



    static void registerFunction(lua_State* L, const std::string& name,
                                 lua_CFunction func);



    static std::string checkError(lua_State* L, int result);
};

}
}
