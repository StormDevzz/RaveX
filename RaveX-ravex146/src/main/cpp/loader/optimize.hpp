#pragma once

#include <string>
#include <vector>
#include <cstdint>

namespace ravex {
namespace loader {

struct OptResult {
    bool ok;
    std::string message;
};

struct OptimizationReport {
    uint64_t freedKB;
    std::vector<OptResult> applied;
    std::vector<OptResult> failed;
};

class SystemOptimizer {
public:
    OptimizationReport runAll();
    OptimizationReport trimMemory();
    OptimizationReport setHighPriority();
    OptimizationReport adjustOOM();
    OptimizationReport cleanPageCache();
    OptimizationReport setCPUGovernor(const std::string& gov);
    OptimizationReport killProcess(int pid);
    OptimizationReport suggestFreeMemory(uint64_t targetMB);

    static std::string formatBytes(uint64_t kb);

private:
    uint64_t readRSS();
};

} 
} 
