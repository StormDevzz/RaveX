#include "antiafk.hpp"

#include <iostream>
#include <cstdlib>
#include <ctime>
#include <chrono>
#include <algorithm>
#include <sstream>

namespace ravex {


std::atomic<bool>                AntiAfk::running_{false};
std::thread                      AntiAfk::worker_;
AfkConfig                        AntiAfk::config_;
std::vector<AfkEvent>            AntiAfk::eventLog_;
std::unique_ptr<InputBackend>    AntiAfk::backend_;


OsBackend detectOs() {
#if defined(_WIN32)
    return OsBackend::Windows;
#elif defined(__linux__)
    return OsBackend::Linux;
#else
    return OsBackend::Unknown;
#endif
}


bool AntiAfk::start(const AfkConfig& config) {
    if (running_.load()) return true;

    backend_.reset(InputBackend::create());
    if (!backend_ || !backend_->init()) {
        std::cerr << "[AntiAFK] No input backend available for OS: "
                  << static_cast<int>(detectOs()) << std::endl;
        backend_.reset();
        return false;
    }

    std::cout << "[AntiAFK] Using backend: " << backend_->name() << std::endl;

    config_ = config;
    running_.store(true);
    eventLog_.clear();
    std::srand(static_cast<unsigned>(std::time(nullptr)));

    worker_ = std::thread(loop);
    logEvent("start", "AntiAFK started (" + backend_->name() +
             ", interval=" + std::to_string(config.intervalMs) + "ms)");
    return true;
}

void AntiAfk::stop() {
    if (!running_.load()) return;
    running_.store(false);
    if (worker_.joinable()) worker_.join();
    logEvent("stop", "AntiAFK stopped");
    if (backend_) {
        backend_->shutdown();
        backend_.reset();
    }
}

bool AntiAfk::isRunning() {
    return running_.load();
}

InputBackend* AntiAfk::backend() {
    return backend_.get();
}


void AntiAfk::loop() {
    while (running_.load()) {
        performRandomAction();

        int jitter = config_.maxJitterMs > 0
                     ? std::rand() % config_.maxJitterMs
                     : 0;
        int totalSleep = config_.intervalMs + jitter;

        for (int i = 0; i < totalSleep / 100 && running_.load(); i++) {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
        }
    }
}


bool AntiAfk::performRandomAction() {
    if (!backend_ || !backend_->isAvailable()) return false;

    enum Action { MOVE_MOUSE, LOOK_AROUND, TAP_KEY, JUMP, CLICK };
    std::vector<Action> pool;
    if (config_.mouseMove)  pool.push_back(MOVE_MOUSE);
    if (config_.lookAround) pool.push_back(LOOK_AROUND);
    if (config_.keyPress)   pool.push_back(TAP_KEY);
    if (config_.jumpSimulation) pool.push_back(JUMP);
    if (config_.mouseClick) pool.push_back(CLICK);

    if (pool.empty()) return false;
    Action action = pool[std::rand() % pool.size()];

    switch (action) {
        case MOVE_MOUSE: {
            int dx = (std::rand() % 100) - 50;
            int dy = (std::rand() % 100) - 50;
            backend_->moveMouse(dx, dy);
            logEvent("mouse", "jitter (" + std::to_string(dx) + ", " + std::to_string(dy) + ")");
            break;
        }
        case LOOK_AROUND: {
            int dx = (std::rand() % (config_.rotationRange * 2)) - config_.rotationRange;
            int dy = (std::rand() % 60) - 30;
            backend_->moveMouse(dx, dy);
            logEvent("look", "rotate (" + std::to_string(dx) + ", " + std::to_string(dy) + ")");
            break;
        }
        case TAP_KEY: {
            std::vector<std::string> keys = {"w", "a", "s", "d", "space"};
            std::string key = keys[std::rand() % keys.size()];
            int dur = 80 + (std::rand() % 200);
            backend_->tapKey(key, dur);
            logEvent("key", "tap " + key + " (" + std::to_string(dur) + "ms)");
            break;
        }
        case JUMP: {
            backend_->tapKey("space", 60 + (std::rand() % 100));
            logEvent("jump", "space tap");
            break;
        }
        case CLICK: {
            int btn = std::rand() % 2;
            backend_->clickMouse(btn);
            logEvent("click", "button " + std::to_string(btn));
            break;
        }
    }
    return true;
}


bool AntiAfk::moveMouse(int dx, int dy) {
    if (!backend_ || !backend_->isAvailable()) return false;
    return backend_->moveMouse(dx, dy);
}

bool AntiAfk::clickMouse(int button) {
    if (!backend_ || !backend_->isAvailable()) return false;
    return backend_->clickMouse(button);
}

bool AntiAfk::tapKey(const std::string& key, int durationMs) {
    if (!backend_ || !backend_->isAvailable()) return false;
    return backend_->tapKey(key, durationMs);
}


void AntiAfk::setConfig(const AfkConfig& config) {
    config_ = config;
    logEvent("config", "config updated (interval=" +
             std::to_string(config.intervalMs) + "ms)");
}

std::vector<AfkEvent> AntiAfk::getEventLog() {
    return eventLog_;
}

void AntiAfk::clearEventLog() {
    eventLog_.clear();
}

void AntiAfk::logEvent(const std::string& type, const std::string& desc) {
    AfkEvent ev{
        type,
        desc,
        std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count()
    };
    eventLog_.push_back(ev);

    auto now = std::chrono::system_clock::to_time_t(
                   std::chrono::system_clock::now());
    std::string timeStr = std::ctime(&now);
    if (!timeStr.empty()) timeStr.pop_back();
    std::cout << "[AntiAFK] " << type << " | " << desc
              << " @ " << timeStr << std::endl;
}

} 
