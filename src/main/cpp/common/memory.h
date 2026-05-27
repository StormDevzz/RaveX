#pragma once

#include <cstdint>
#include <string>
#include <unordered_map>

namespace ravex {

struct MemInfo {
    uint64_t total_kb;
    uint64_t free_kb;
    uint64_t avail_kb;
    uint64_t cached_kb;
};

class Memory {
public:
    static MemInfo readMemInfo();
    static bool trimAllocator();
    static uint64_t processRSS();

    static bool setProcessPriority(int niceValue);
    static bool setThreadAffinity(int cpuCore);

private:
    static std::unordered_map<std::string, std::string> parseProcMeminfo();
};

} // namespace ravex
