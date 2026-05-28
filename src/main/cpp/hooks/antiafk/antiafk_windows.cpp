#include "antiafk_input.h"

#include <iostream>
#include <thread>
#include <chrono>
#include <unordered_map>
#include <cstdlib>

#ifdef _WIN32
#include <windows.h>
#endif

namespace ravex {

class WindowsInputBackend : public InputBackend {
public:
    WindowsInputBackend() : initialized_(false) {}
    ~WindowsInputBackend() override { shutdown(); }

    bool init() override {
#ifdef _WIN32
        initialized_ = true;
        std::cout << "[AntiAFK:Windows] Initialized WinAPI input" << std::endl;
        return true;
#else
        std::cerr << "[AntiAFK:Windows] Not a Windows platform" << std::endl;
        return false;
#endif
    }

    void shutdown() override {
        initialized_ = false;
    }

    bool isAvailable() override { return initialized_; }
    std::string name() override { return "WinAPI (Windows)"; }

    bool moveMouse(int dx, int dy) override {
#ifdef _WIN32
        INPUT input = {};
        input.type = INPUT_MOUSE;
        input.mi.dx = dx;
        input.mi.dy = dy;
        input.mi.dwFlags = MOUSEEVENTF_MOVE;
        SendInput(1, &input, sizeof(INPUT));
        return true;
#else
        return false;
#endif
    }

    bool absMoveMouse(int x, int y) override {
#ifdef _WIN32
        int screenW = GetSystemMetrics(SM_CXSCREEN);
        int screenH = GetSystemMetrics(SM_CYSCREEN);
        INPUT input = {};
        input.type = INPUT_MOUSE;
        input.mi.dx = (x * 65535) / screenW;
        input.mi.dy = (y * 65535) / screenH;
        input.mi.dwFlags = MOUSEEVENTF_ABSOLUTE | MOUSEEVENTF_MOVE;
        SendInput(1, &input, sizeof(INPUT));
        return true;
#else
        return false;
#endif
    }

    bool clickMouse(int button) override {
#ifdef _WIN32
        INPUT down = {}, up = {};
        down.type = up.type = INPUT_MOUSE;
        DWORD flag = (button == 0) ? MOUSEEVENTF_LEFTDOWN : MOUSEEVENTF_RIGHTDOWN;
        DWORD flagUp = (button == 0) ? MOUSEEVENTF_LEFTUP : MOUSEEVENTF_RIGHTUP;
        down.mi.dwFlags = flag;
        up.mi.dwFlags = flagUp;
        SendInput(1, &down, sizeof(INPUT));
        SendInput(1, &up, sizeof(INPUT));
        return true;
#else
        return false;
#endif
    }

    bool pressKey(const std::string& key) override {
#ifdef _WIN32
        WORD vk = keyToVk(key);
        if (!vk) return false;
        INPUT input = {};
        input.type = INPUT_KEYBOARD;
        input.ki.wVk = vk;
        SendInput(1, &input, sizeof(INPUT));
        return true;
#else
        return false;
#endif
    }

    bool releaseKey(const std::string& key) override {
#ifdef _WIN32
        WORD vk = keyToVk(key);
        if (!vk) return false;
        INPUT input = {};
        input.type = INPUT_KEYBOARD;
        input.ki.wVk = vk;
        input.ki.dwFlags = KEYEVENTF_KEYUP;
        SendInput(1, &input, sizeof(INPUT));
        return true;
#else
        return false;
#endif
    }

    bool tapKey(const std::string& key, int durationMs) override {
        if (!pressKey(key)) return false;
        std::this_thread::sleep_for(std::chrono::milliseconds(durationMs));
        releaseKey(key);
        return true;
    }

private:
    bool initialized_;

#ifdef _WIN32
    WORD keyToVk(const std::string& key) {
        static const std::unordered_map<std::string, WORD> map = {
            {"w", 0x57}, {"a", 0x41}, {"s", 0x53}, {"d", 0x44},
            {"space", VK_SPACE}, {"jump", VK_SPACE},
            {"forward", 0x57}, {"back", 0x53},
            {"left", 0x41}, {"right", 0x44},
            {"shift", VK_SHIFT}, {"ctrl", VK_CONTROL},
            {"tab", VK_TAB}, {"e", 0x45},
            {"1", 0x31}, {"2", 0x32}, {"3", 0x33},
            {"4", 0x34}, {"5", 0x35}, {"6", 0x36},
            {"7", 0x37}, {"8", 0x38}, {"9", 0x39},
        };
        auto it = map.find(key);
        return it != map.end() ? it->second : 0;
    }
#endif
};

InputBackend* InputBackend::create() {
    return new WindowsInputBackend();
}

} // namespace ravex
