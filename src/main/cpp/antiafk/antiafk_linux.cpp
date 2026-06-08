#include "antiafk_input.h"

#include <iostream>
#include <cstdlib>
#include <cstring>
#include <sstream>

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/XTest.h>

namespace ravex {

class LinuxInputBackend : public InputBackend {
public:
    LinuxInputBackend() : display_(nullptr) {}
    ~LinuxInputBackend() override { shutdown(); }

    bool init() override {
        if (display_) return true;
        display_ = XOpenDisplay(nullptr);
        if (!display_) {
            std::cerr << "[AntiAFK:Linux] Cannot open X display (DISPLAY="
                      << (getenv("DISPLAY") ? getenv("DISPLAY") : "unset")
                      << ")" << std::endl;
            return false;
        }

        int eventBase, errorBase, majorVersion, minorVersion;
        if (!XTestQueryExtension(display_, &eventBase, &errorBase, &majorVersion, &minorVersion)) {
            std::cerr << "[AntiAFK:Linux] XTest extension not available" << std::endl;
            XCloseDisplay(display_);
            display_ = nullptr;
            return false;
        }

        std::cout << "[AntiAFK:Linux] Initialized XTest on "
                  << DisplayString(display_) << std::endl;
        return true;
    }

    void shutdown() override {
        if (display_) {
            XCloseDisplay(display_);
            display_ = nullptr;
        }
    }

    bool isAvailable() override { return display_ != nullptr; }
    std::string name() override { return "XTest (Linux)"; }

    bool moveMouse(int dx, int dy) override {
        if (!display_) return false;
        XTestFakeRelativeMotionEvent(display_, dx, dy, CurrentTime);
        XFlush(display_);
        return true;
    }

    bool absMoveMouse(int x, int y) override {
        if (!display_) return false;
        XTestFakeMotionEvent(display_, -1, x, y, CurrentTime);
        XFlush(display_);
        return true;
    }

    bool clickMouse(int button) override {
        if (!display_) return false;
        int btn = (button == 0) ? Button1 : Button3;
        XTestFakeButtonEvent(display_, btn, True, CurrentTime);
        XFlush(display_);
        XTestFakeButtonEvent(display_, btn, False, CurrentTime);
        XFlush(display_);
        return true;
    }

    bool pressKey(const std::string& key) override {
        if (!display_) return false;
        KeyCode code = lookupKey(key);
        if (!code) return false;
        XTestFakeKeyEvent(display_, code, True, CurrentTime);
        XFlush(display_);
        return true;
    }

    bool releaseKey(const std::string& key) override {
        if (!display_) return false;
        KeyCode code = lookupKey(key);
        if (!code) return false;
        XTestFakeKeyEvent(display_, code, False, CurrentTime);
        XFlush(display_);
        return true;
    }

    bool tapKey(const std::string& key, int durationMs) override {
        if (!pressKey(key)) return false;
        struct timespec ts = {durationMs / 1000, (durationMs % 1000) * 1000000L};
        nanosleep(&ts, nullptr);
        releaseKey(key);
        return true;
    }

private:
    Display* display_;

    KeyCode lookupKey(const std::string& key) {
        KeyCode code = XKeysymToKeycode(display_, XStringToKeysym(key.c_str()));
        if (code) return code;
        static const std::pair<const char*, KeySym> fallbacks[] = {
            {"space", XK_space}, {"w", XK_w}, {"a", XK_a},
            {"s", XK_s}, {"d", XK_d},
            {"jump", XK_space}, {"forward", XK_w},
            {"back", XK_s}, {"left", XK_a}, {"right", XK_d},
        };
        for (auto& [name, sym] : fallbacks) {
            if (key == name) return XKeysymToKeycode(display_, sym);
        }
        return 0;
    }
};

InputBackend* InputBackend::create() {
    return new LinuxInputBackend();
}

} // namespace ravex
