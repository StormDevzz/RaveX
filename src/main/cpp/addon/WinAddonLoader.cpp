#include "include/WinAddonLoader.h"
#ifdef _WIN32
#include <windows.h>
#endif

namespace ravex {
namespace addon {

void* WinAddonLoader::loadLibrary(const std::string& path) {
#ifdef _WIN32
    return (void*)LoadLibraryA(path.c_str());
#else
    return nullptr;
#endif
}

void* WinAddonLoader::getSymbol(void* handle, const std::string& symbol) {
#ifdef _WIN32
    return (void*)GetProcAddress((HMODULE)handle, symbol.c_str());
#else
    return nullptr;
#endif
}

void WinAddonLoader::unloadLibrary(void* handle) {
#ifdef _WIN32
    FreeLibrary((HMODULE)handle);
#endif
}

std::string WinAddonLoader::getLastError() {
#ifdef _WIN32
    DWORD err = GetLastError();
    return "Win32 Error Code: " + std::to_string(err);
#else
    return "Not Windows platform";
#endif
}

}
}
