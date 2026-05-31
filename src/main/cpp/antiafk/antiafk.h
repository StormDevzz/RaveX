#pragma once

#include "antiafk_common.h"
#include "antiafk_input.h"
#include <memory>
#include <thread>
#include <atomic>

namespace ravex {

class AntiAfk {
public:
    static bool start(const AfkConfig& config);
    static void stop();
    static bool isRunning();

    static bool performRandomAction();
    static bool moveMouse(int dx, int dy);
    static bool clickMouse(int button);
    static bool tapKey(const std::string& key, int durationMs);

    static std::vector<AfkEvent> getEventLog();
    static void clearEventLog();
    static void setConfig(const AfkConfig& config);

    static InputBackend* backend();

private:
    static void loop();
    static void logEvent(const std::string& type, const std::string& desc);

    static std::atomic<bool>          running_;
    static std::thread                worker_;
    static AfkConfig                  config_;
    static std::vector<AfkEvent>      eventLog_;
    static std::unique_ptr<InputBackend> backend_;
};

} // namespace ravex
