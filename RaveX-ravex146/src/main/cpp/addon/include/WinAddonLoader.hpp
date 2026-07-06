#pragma once
#include "AddonLoader.hpp"

namespace ravex {
namespace addon {

class WinAddonLoader : public AddonLoader {
public:
    void* loadLibrary(const std::string& path) override;
    void* getSymbol(void* handle, const std::string& symbol) override;
    void unloadLibrary(void* handle) override;
    std::string getLastError() override;
};

}
}
