#include "memory.hpp"

#include <fstream>
#include <sstream>
#include <iostream>
#include <algorithm>
#include <thread>
#include <vector>
#include <cstring>
#include <cerrno>

#ifdef _WIN32
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <psapi.h>
#pragma comment(lib, "psapi.lib")
#else
#include <unistd.h>
#include <sys/resource.h>
#include <sys/syscall.h>
#include <sched.h>
#include <malloc.h>
#include <fcntl.h>
#include <sys/mman.h>
#endif

namespace ravex {

#ifndef _WIN32
static std::unordered_map<std::string, std::string> parseProcMeminfo() {
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
#endif

MemInfo Memory::readMemInfo() {
    MemInfo info{};
#ifdef _WIN32
    MEMORYSTATUSEX ms;
    ms.dwLength = sizeof(ms);
    if (GlobalMemoryStatusEx(&ms)) {
        info.total_kb  = ms.ullTotalPhys / 1024;
        info.free_kb   = ms.ullAvailPhys / 1024;
        info.avail_kb  = ms.ullAvailPhys / 1024;
        info.cached_kb = 0;
    }
#else
    auto data = parseProcMeminfo();
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
#endif
    return info;
}

SystemStats Memory::readSystemStats() {
    SystemStats s{};
    s.mem      = readMemInfo();
    s.rss_kb   = processRSS();
    s.nice     = getProcessPriority();
#ifdef _WIN32
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    s.cpu_count = static_cast<int>(si.dwNumberOfProcessors);
#else
    s.cpu_count = static_cast<int>(sysconf(_SC_NPROCESSORS_ONLN));
#endif
    s.oom_adj_applied = false;
    return s;
}

bool Memory::trimAllocator() {
#ifdef _WIN32
    return false;
#else
    return malloc_trim(0) == 1;
#endif
}

bool Memory::releaseFreePages() {
#ifdef _WIN32
    return false;
#else
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
#endif
}

uint64_t Memory::processRSS() {
#ifdef _WIN32
    HANDLE h = GetCurrentProcess();
    PROCESS_MEMORY_COUNTERS pmc;
    if (GetProcessMemoryInfo(h, &pmc, sizeof(pmc))) {
        return pmc.WorkingSetSize / 1024;
    }
    return 0;
#else
    std::ifstream statm("/proc/self/statm");
    if (!statm.is_open()) return 0;
    uint64_t rssPages = 0;
    statm >> rssPages;
    statm >> rssPages;
    return rssPages * sysconf(_SC_PAGESIZE) / 1024;
#endif
}

bool Memory::setProcessPriority(int niceValue) {
#ifdef _WIN32
    DWORD priorityClass;
    if (niceValue <= -15)      priorityClass = HIGH_PRIORITY_CLASS;
    else if (niceValue <= -5)  priorityClass = ABOVE_NORMAL_PRIORITY_CLASS;
    else if (niceValue <= 0)   priorityClass = NORMAL_PRIORITY_CLASS;
    else if (niceValue <= 10)  priorityClass = BELOW_NORMAL_PRIORITY_CLASS;
    else                       priorityClass = IDLE_PRIORITY_CLASS;
    return SetPriorityClass(GetCurrentProcess(), priorityClass) != 0;
#else
    return setpriority(PRIO_PROCESS, 0, niceValue) == 0;
#endif
}

bool Memory::setIONice(int ioClass, int ioPriority) {
#ifdef _WIN32
    return false;
#else
    int ioprio_val = (ioClass << 13) | ioPriority;
    return syscall(SYS_ioprio_set, 1, 0, ioprio_val) == 0;
#endif
}

bool Memory::setOOMScoreAdj(int score) {
#ifdef _WIN32
    return false;
#else
    std::ofstream oom("/proc/self/oom_score_adj");
    if (!oom.is_open()) return false;
    oom << score;
    return true;
#endif
}

bool Memory::setThreadAffinity(int cpuCore) {
#ifdef _WIN32
    HANDLE h = GetCurrentThread();
    ULONG_PTR mask = 1ULL << cpuCore;
    return SetThreadAffinityMask(h, mask) != 0;
#else
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    CPU_SET(cpuCore, &cpuset);
    return sched_setaffinity(0, sizeof(cpuset), &cpuset) == 0;
#endif
}

bool Memory::setCPUSchedPolicy(int policy, int priority) {
#ifdef _WIN32
    return false;
#else
    struct sched_param param{};
    param.sched_priority = priority;
    return sched_setscheduler(0, policy, &param) == 0;
#endif
}

int Memory::getProcessPriority() {
#ifdef _WIN32
    DWORD pc = GetPriorityClass(GetCurrentProcess());
    if (pc == HIGH_PRIORITY_CLASS)         return -15;
    if (pc == ABOVE_NORMAL_PRIORITY_CLASS) return -5;
    if (pc == BELOW_NORMAL_PRIORITY_CLASS) return 10;
    if (pc == IDLE_PRIORITY_CLASS)         return 20;
    return 0;
#else
    return getpriority(PRIO_PROCESS, 0);
#endif
}

bool Memory::advisePageOut(void* addr, size_t len) {
#ifdef _WIN32
    (void)addr; (void)len;
    return false;
#else
    return madvise(addr, len, MADV_PAGEOUT) == 0;
#endif
}

bool Memory::adviseCold(void* addr, size_t len) {
#ifdef _WIN32
    (void)addr; (void)len;
    return false;
#else
    return madvise(addr, len, MADV_COLD) == 0;
#endif
}

bool Memory::adviseDontNeed(void* addr, size_t len) {
#ifdef _WIN32
    (void)addr; (void)len;
    return false;
#else
    return madvise(addr, len, MADV_DONTNEED) == 0;
#endif
}

bool Memory::dropCaches() {
#ifdef _WIN32
    return false;
#else
    int fd = open("/proc/sys/vm/drop_caches", O_WRONLY);
    if (fd < 0) return false;
    bool ok = write(fd, "3", 1) == 1;
    close(fd);
    return ok;
#endif
}

bool Memory::compactMemory() {
#ifdef _WIN32
    return false;
#else
    int fd = open("/proc/sys/vm/compact_memory", O_WRONLY);
    if (fd < 0) return false;
    bool ok = write(fd, "1", 1) == 1;
    close(fd);
    return ok;
#endif
}

} 
