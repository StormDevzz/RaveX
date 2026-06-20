#include "include/LinuxAddonLoader.h"
#ifndef _WIN32
#include <dlfcn.h>
#endif

namespace ravex {
namespace addon {

void* LinuxAddonLoader::loadLibrary(const std::string& path) {
#ifndef _WIN32
    return dlopen(path.c_str(), RTLD_LAZY);
#else
    return nullptr;
#endif
}

void* LinuxAddonLoader::getSymbol(void* handle, const std::string& symbol) {
#ifndef _WIN32
    return dlsym(handle, symbol.c_str());
#else
    return nullptr;
#endif
}

void LinuxAddonLoader::unloadLibrary(void* handle) {
#ifndef _WIN32
    dlclose(handle);
#endif
}

std::string LinuxAddonLoader::getLastError() {
#ifndef _WIN32
    char* err = dlerror();
    return err ? std::string(err) : "No error";
#else
    return "Windows platform";
#endif
}

}
}
