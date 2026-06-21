#pragma once

#ifdef _WIN32
    #define PLATFORM_WINDOWS 1
#elif defined(__linux__)
    #define PLATFORM_LINUX 1
#else
    #error "Unsupported platform"
#endif

#include <cstdint>

namespace shieldfucker {

bool isKeyPressed(int key);

void sleepMs(uint64_t ms);

uint64_t currentTimeMs();

} // namespace shieldfucker
