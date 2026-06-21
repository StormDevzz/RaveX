#pragma once
#include <string>

namespace ravex {
namespace plugins {
namespace optimize {

class StartupBoost {
public:
    static void boostAll();
    static void boostProcessPriority();
    static void boostThreadPriority();
    static void setTimerResolution();
    static void preventPowerThrottling();
    static void bindToPerformanceCores();
    static void setGpuPriority();
    static void restoreAll();
};

class MemoryBoost {
public:
    static bool optimizeAll();
    static bool trimWorkingSet();
    static bool setWorkingSetLimit();
    static bool enableLowFragmentationHeap();
    static bool enableLargePages();
    static bool setMemoryPriority();
    static void logMemoryStats();
};

class SystemConfig {
public:
    static bool checkAll();
    static bool isHighPerformancePowerPlan();
    static bool isGameModeEnabled();
    static bool isCoreIsolationEnabled();
    static bool isRunningOnSsd();
    static void disableNagle();
    static std::string getSystemInfo();
};

} // namespace optimize
} // namespace plugins
} // namespace ravex
