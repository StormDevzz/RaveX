#include "memory.h"

#include <fstream>
#include <sstream>
#include <iostream>
#include <algorithm>
#include <thread>
#include <vector>
#include <cstring>
#include <unistd.h>
#include <sys/resource.h>
#include <sched.h>
#include <malloc.h>

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

bool Memory::trimAllocator() {
    return malloc_trim(0) == 1;
}

uint64_t Memory::processRSS() {
    std::ifstream statm("/proc/self/statm");
    if (!statm.is_open()) return 0;
    uint64_t rssPages = 0;
    statm >> rssPages; // skip first (size)
    statm >> rssPages;
    return rssPages * sysconf(_SC_PAGESIZE) / 1024;
}

bool Memory::setProcessPriority(int niceValue) {
    return setpriority(PRIO_PROCESS, 0, niceValue) == 0;
}

bool Memory::setThreadAffinity(int cpuCore) {
    cpu_set_t cpuset;
    CPU_ZERO(&cpuset);
    CPU_SET(cpuCore, &cpuset);
    return sched_setaffinity(0, sizeof(cpuset), &cpuset) == 0;
}

} // namespace ravex
