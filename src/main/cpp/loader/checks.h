#pragma once

#include <string>
#include <vector>
#include <cstdint>

namespace ravex {
namespace loader {

struct ProcessInfo {
    int pid;
    std::string name;
    double memMB;
    double cpuPct;
};

struct SystemReport {
    std::string osName;
    std::string osVersion;
    std::string osArch;
    int cpuCores;
    double cpuLoad;
    double cpuTemp;
    uint64_t totalRamKB;
    uint64_t freeRamKB;
    uint64_t availRamKB;
    uint64_t swapTotalKB;
    uint64_t swapFreeKB;
    uint64_t diskFreeKB;
    uint64_t diskTotalKB;
    uint64_t selfRSSKB;
    int processCount;
    double loadAvg1m;
    std::string cpuGovernor;
    std::vector<ProcessInfo> topProcesses;
    int score;
    std::vector<std::string> warnings;
};

class SystemChecks {
public:
    SystemReport runAll();
    void printReport(const SystemReport& r);

private:
    void readProcMemInfo(uint64_t& total, uint64_t& free, uint64_t& avail, uint64_t& swapTotal, uint64_t& swapFree);
    double readCPUTemp();
    std::string readCPUGovernor();
    double readLoadAvg();
    void readTopProcesses(std::vector<ProcessInfo>& out);
    int countProcesses();
    uint64_t readSelfRSS();
    void calcScore(SystemReport& r);
};

} 
} 
