#pragma once

#include "antiafk_common.hpp"
#include <string>
#include <vector>

namespace ravex {

class InputBackend {
public:
    virtual ~InputBackend() = default;

    virtual bool init() = 0;
    virtual void shutdown() = 0;
    virtual bool isAvailable() = 0;
    virtual std::string name() = 0;

    virtual bool moveMouse(int dx, int dy) = 0;
    virtual bool absMoveMouse(int x, int y) = 0;
    virtual bool clickMouse(int button) = 0;
    virtual bool pressKey(const std::string& key) = 0;
    virtual bool releaseKey(const std::string& key) = 0;
    virtual bool tapKey(const std::string& key, int durationMs) = 0;

    static InputBackend* create();
};

} 
