#pragma once

#include <string>
#include <vector>
#include <functional>

namespace ravex {
namespace lua {





struct LuaAddonMeta {
    std::string name;
    std::string author;
    std::string version;
    std::string description;
    int apiLevel = 1;
};



struct LuaModuleInfo {
    std::string name;
    std::string description;
    bool enabled = false;
};



struct LuaLoadResult {
    bool success = false;
    std::string errorMsg;
    std::string filePath;
};



enum class LuaLogLevel {
    Debug,
    Info,
    Warn,
    Error
};



enum class LuaValueType {
    Nil,
    Boolean,
    Number,
    String,
    Table,
    Function
};

} 
} 
