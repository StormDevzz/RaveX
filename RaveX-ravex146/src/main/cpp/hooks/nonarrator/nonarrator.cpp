#include "nonarrator.hpp"
#include <atomic>

static std::atomic<bool> g_narratorForced{false};

#ifdef _WIN32
#include <windows.h>
static void sendNarratorToggle() {
    INPUT inputs[4] = {};

    inputs[0].type = INPUT_KEYBOARD;
    inputs[0].ki.wVk = VK_CONTROL;

    inputs[1].type = INPUT_KEYBOARD;
    inputs[1].ki.wVk = 'B';

    inputs[2].type = INPUT_KEYBOARD;
    inputs[2].ki.wVk = 'B';
    inputs[2].ki.dwFlags = KEYEVENTF_KEYUP;

    inputs[3].type = INPUT_KEYBOARD;
    inputs[3].ki.wVk = VK_CONTROL;
    inputs[3].ki.dwFlags = KEYEVENTF_KEYUP;

    SendInput(4, inputs, sizeof(INPUT));
}
#else
#include <cstdlib>
static void sendNarratorToggle() {


}
#endif

void forceNarratorOff() {
    g_narratorForced = true;
#ifdef _WIN32
    sendNarratorToggle();
#endif
}

bool isNarratorForced() {
    return g_narratorForced.load();
}
