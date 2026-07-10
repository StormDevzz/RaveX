#include "shieldfucker_platform.hpp"

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#elif defined(__linux__)
#include <unistd.h>
#include <chrono>
#include <thread>
#endif

namespace shieldfucker {

bool isKeyPressed(int key) {
#ifdef _WIN32
    return (GetAsyncKeyState(key) & 0x8000) != 0;
#else
    (void)key;
    return false;
#endif
}

void sleepMs(uint64_t ms) {
#ifdef _WIN32
    Sleep(static_cast<DWORD>(ms));
#else
    std::this_thread::sleep_for(std::chrono::milliseconds(ms));
#endif
}

uint64_t currentTimeMs() {
#ifdef _WIN32
    return static_cast<uint64_t>(GetTickCount64());
#else
    auto now = std::chrono::steady_clock::now();
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        now.time_since_epoch()).count();
#endif
}

}
