#pragma once

#include "LuaAddon.hpp"
#include <string>
#include <vector>
#include <memory>

namespace ravex {
namespace addon {
    class AddonContext;
}

namespace lua {






















class LuaLoader {
private:


    std::vector<std::unique_ptr<LuaAddon>> m_addons;



    std::string m_addonDir;

public:




    explicit LuaLoader(const std::string& dir = "addons/lua");

    ~LuaLoader();











    int loadAll(ravex::addon::AddonContext* ctx);



    bool loadFile(const std::string& filePath,
                  ravex::addon::AddonContext* ctx);



    void unloadAll();



    void tickAll();




    void eventAll(const std::string& eventName);



    size_t count() const { return m_addons.size(); }



    LuaAddon* getAddon(size_t index) const;





    LuaAddon* findAddon(const std::string& name) const;

private:


    std::vector<std::string> scanDirectory();
};

}
}
