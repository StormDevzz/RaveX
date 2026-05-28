#include "memory.h"

#include <fstream>
#include <sstream>
#include <iostream>
#include <algorithm>
#include <thread>
#include <vector>
#include <cstring>
#include <cerrno>
#include <unistd.h>
#include <sys/resource.h>
#include <sys/syscall.h>
#include <sched.h>
#include <malloc.h>
#include <fcntl.h>
#include <sys/mman.h>

namespace ravex {

std::unordered_map<std::string, std::string> Memory::parseProcMeminfo() {
    std::unordered_map<std::string, std::string> result;
    std::ifstream meminfo("/proc/meminfo");
    std::string line;
    while (std::getline(meminfo, line)) {
        auto colon = line.find(':');
        if (colon == std::string::npos) continue;
        std::string key = line.substr(0, colon);
        std::string val = line.substr(colon + 1);
        val.erase(0, val.find_first_not_of(" \t"));
        result[key] = val;
    }
    return result;
}

MemInfo Memory::readMemInfo() {
    auto data = parseProcMeminfo();
    MemInfo info{};
    auto parse = [&](const std::string& k) -> uint64_t {
        auto it = data.find(k);
        if (it == data.end()) return 0;
        std::string s = it->second;
        s.erase(std::remove(s.begin(), s.end(), ' '), s.end());
        s.erase(s.find("kB"), std::string::npos);
        try { return std::stoull(s); } catch (...) { return 0; }
    };
    info.total_kb  = parse("MemTotal");
    info.free_kb   = parse("MemFree");
    info.avail_kb  = parse("MemAvailable");
    info.cached_kb = parse("Cached");
    return info;
}

SystemStats Memory::readSystemStats() {
    SystemStats s{};
    s.mem      = readMemInfo();
    s.rss_kb   = processRSS();
    s.nice     = getProcessPriority();
    s.cpu_count = static_cast<int>(sysconf(_SC_NPROCESSORS_ONLN));
    s.oom_adj_applied = false;
    return s;
}

bool Memory::trimAllocator() {
    return malloc_trim(0) == 1;
}

bool Memory::releaseFreePages() {
    for (int i = 0; i < 5; i++) {
        malloc_trim(0);
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    int fd = open("/proc/self/oom_score_adj", O_WRONLY);
    if (fd >= 0) {
        write(fd, "-500", 4);
        close(fd);
    }
    return true;
}

uint64_t Memory::processRSS() {
    std::ifstream statm("/proc/self/statm");
    if (!statm.is_open()) return 0;
    uint64_t rssPages = 0;
    statm >> rssPages;
    statm >> rssPages;
    return rssPages * sysconf(_SC_PAGESIZE) / 1024;
}

bool Memory::setProcessPriority(int niceValue) {
    return setpriority(PRIO_PROCESS, 0, niceValue) == 0;
}

bool Memory::setIONice(int ioClass, int ioPriority) {
    int ioprio_val = (ioClass << 13) | ioPriority;
    return syscall(SYS_ioprio_set, 1, 0, ioprio_val) == 0;
}

bool Memory::setOOMScoreAdj(int score) {
    std::ofstream oom("/proc/self/oom_score_adj");
    if (!oom.is_open()) return false;
    oom << score;
    return true;
}

bool Memory::setThreadAffinity(int cpuCore) {
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    CPU_SET(cpuCore, &cpuset);
    return sched_setaffinity(0, sizeof(cpuset), &cpuset) == 0;
}

bool Memory::setCPUSchedPolicy(int policy, int priority) {
    struct sched_param param{};
    param.sched_priority = priority;
    return sched_setscheduler(0, policy, &param) == 0;
}

int Memory::getProcessPriority() {
    return getpriority(PRIO_PROCESS, 0);
}

bool Memory::advisePageOut(void* addr, size_t len) {
    return madvise(addr, len, MADV_PAGEOUT) == 0;
}

bool Memory::adviseCold(void* addr, size_t len) {
    return madvise(addr, len, MADV_COLD) == 0;
}

bool Memory::adviseDontNeed(void* addr, size_t len) {
    return madvise(addr, len, MADV_DONTNEED) == 0;
}

bool Memory::dropCaches() {
    int fd = open("/proc/sys/vm/drop_caches", O_WRONLY);
    if (fd < 0) return false;
    bool ok = write(fd, "3", 1) == 1;
    close(fd);
    return ok;
}

bool Memory::compactMemory() {
    int fd = open("/proc/sys/vm/compact_memory", O_WRONLY);
    if (fd < 0) return false;
    bool ok = write(fd, "1", 1) == 1;
    close(fd);
    return ok;
}

} // namespace ravex
