#include "optimizer.h"
#include <iostream>
#include <thread>
#include <chrono>

namespace ravex {

Optimizer::Result Optimizer::run(const std::string& mode) {
    if (mode == "Aggressive") return aggressive();
    if (mode == "Normal")     return normal();
    if (mode == "Soft")       return soft();
    return {false, "unknown mode", 0};
}

Optimizer::Result Optimizer::aggressive() {
    Memory::setProcessPriority(-15);
    Memory::setThreadAffinity(0);

    for (int i = 0; i < 3; i++) {
        Memory::trimAllocator();
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
    }

    auto info = Memory::readMemInfo();
    return {true, "Aggressive optimization complete", info.free_kb};
}

Optimizer::Result Optimizer::normal() {
    Memory::setProcessPriority(-10);
    Memory::trimAllocator();

    auto info = Memory::readMemInfo();
    return {true, "Normal optimization complete", info.free_kb};
}

Optimizer::Result Optimizer::soft() {
    auto info = Memory::readMemInfo();
    double usedPercent = 100.0 * (1.0 - (double)info.free_kb / (double)info.total_kb);

    if (usedPercent > 65.0) {
        Memory::trimAllocator();
    }

    info = Memory::readMemInfo();
    return {true, "Soft optimization complete", info.free_kb};
}

} // namespace ravex
