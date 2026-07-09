#pragma once
#include <string>

namespace ravex {
namespace addon {

class AddonLoader {
public:
    virtual ~AddonLoader() = default;
    virtual void* loadLibrary(const std::string& path) = 0;
    virtual void* getSymbol(void* handle, const std::string& symbol) = 0;
    virtual void unloadLibrary(void* handle) = 0;
    virtual std::string getLastError() = 0;
};

}
}
