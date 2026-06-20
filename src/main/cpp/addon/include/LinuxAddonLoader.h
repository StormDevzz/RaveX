#pragma once
#include "AddonLoader.h"

namespace ravex {
namespace addon {

class LinuxAddonLoader : public AddonLoader {
public:
    void* loadLibrary(const std::string& path) override;
    void* getSymbol(void* handle, const std::string& symbol) override;
    void unloadLibrary(void* handle) override;
    std::string getLastError() override;
};

}
}
