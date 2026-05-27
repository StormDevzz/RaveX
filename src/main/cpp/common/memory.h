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

struct SystemStats {
    MemInfo mem;
    uint64_t rss_kb;
    int      nice;
    int      cpu_count;
    bool     oom_adj_applied;
};

class Memory {
public:
    static MemInfo          readMemInfo();
    static SystemStats      readSystemStats();
    static bool             trimAllocator();
    static bool             releaseFreePages();
    static uint64_t         processRSS();

    static bool             setProcessPriority(int niceValue);
    static bool             setIONice(int ioClass, int ioPriority);
    static bool             setOOMScoreAdj(int score);
    static bool             setThreadAffinity(int cpuCore);
    static bool             setCPUSchedPolicy(int policy, int priority);
    static int              getProcessPriority();

    static bool             advisePageOut(void* addr, size_t len);
    static bool             adviseCold(void* addr, size_t len);
    static bool             adviseDontNeed(void* addr, size_t len);

    static bool             dropCaches();
    static bool             compactMemory();

private:
    static std::unordered_map<std::string, std::string> parseProcMeminfo();
};

} // namespace ravex
