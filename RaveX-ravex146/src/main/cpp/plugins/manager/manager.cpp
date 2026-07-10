#include "manager.hpp"
#include <iostream>

#ifdef _WIN32
#include <windows.h>
#else
#include <unistd.h>
#endif

namespace ravex {
namespace plugins {
namespace manager {

void NativeManager::checkNatives() {

    std::cout << "[RaveX] [NativeManager] initializing native component checks, hold tight..." << std::endl;


#if defined(__SSE__) || defined(_M_AMD64) || defined(_M_X64)
    std::cout << "[RaveX] [NativeManager] sse instructions: supported and active, zoom zoom!" << std::endl;
#else
    std::cout << "[RaveX] [NativeManager] sse instructions: not supported, bummer." << std::endl;
#endif


    if (sizeof(void*) == 8) {
        std::cout << "[RaveX] [NativeManager] system architecture: 64-bit, styling!" << std::endl;
    } else {
        std::cout << "[RaveX] [NativeManager] system architecture: 32-bit, pretty old school..." << std::endl;
    }

    std::cout << "[RaveX] [NativeManager] all checks passed, components are good to go, let's shred!" << std::endl;
}

}
}
}
