#include "optimize.hpp"

#ifdef _WIN32
#include <windows.h>
#include <psapi.h>
#include <heapapi.h>
#else
#include <fstream>
#include <iostream>
#include <thread>
#include <cstring>
#include <unistd.h>
#include <fcntl.h>
#include <sys/resource.h>
#include <sys/syscall.h>
#include <malloc.h>
#include <csignal>
#endif

namespace ravex {
namespace loader {

uint64_t SystemOptimizer::readRSS() {
#ifdef _WIN32
    HANDLE h = GetCurrentProcess();
    PROCESS_MEMORY_COUNTERS pmc;
    if (GetProcessMemoryInfo(h, &pmc, sizeof(pmc))) {
        return pmc.WorkingSetSize / 1024;
    }
    return 0;
#else
    std::ifstream f("/proc/self/statm");
    if (!f.is_open()) return 0;
    uint64_t rss = 0;
    f >> rss;
    f >> rss;
    return rss * sysconf(_SC_PAGESIZE) / 1024;
#endif
}

std::string SystemOptimizer::formatBytes(uint64_t kb) {
    if (kb >= 1048576) return std::to_string(kb / 1048576) + " GB";
    if (kb >= 1024) return std::to_string(kb / 1024) + " MB";
    return std::to_string(kb) + " KB";
}

OptimizationReport SystemOptimizer::runAll() {
    OptimizationReport rep;
    uint64_t before = readRSS();

    auto r1 = trimMemory();
    rep.applied.insert(rep.applied.end(), r1.applied.begin(), r1.applied.end());
    rep.failed.insert(rep.failed.end(), r1.failed.begin(), r1.failed.end());

    auto r2 = setHighPriority();
    rep.applied.insert(rep.applied.end(), r2.applied.begin(), r2.applied.end());
    rep.failed.insert(rep.failed.end(), r2.failed.begin(), r2.failed.end());

    auto r3 = adjustOOM();
    rep.applied.insert(rep.applied.end(), r3.applied.begin(), r3.applied.end());
    rep.failed.insert(rep.failed.end(), r3.failed.begin(), r3.failed.end());

    auto r4 = cleanPageCache();
    rep.applied.insert(rep.applied.end(), r4.applied.begin(), r4.applied.end());
    rep.failed.insert(rep.failed.end(), r4.failed.begin(), r4.failed.end());

    uint64_t after = readRSS();
    rep.freedKB = (after < before) ? (before - after) : 0;

    return rep;
}

OptimizationReport SystemOptimizer::trimMemory() {
    OptimizationReport r;
#ifdef _WIN32
    HANDLE h = GetCurrentProcess();
    for (int i = 0; i < 3; i++) {
        SetProcessWorkingSetSize(h, (SIZE_T)-1, (SIZE_T)-1);
        Sleep(10);
    }
    _heapmin();
    r.applied.push_back({true, "Heap trimmed via SetProcessWorkingSetSize and _heapmin()"});
#else
    for (int i = 0; i < 3; i++) {
        malloc_trim(0);
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    r.applied.push_back({true, "Heap trimmed via malloc_trim()"});

    if (mallopt(M_TRIM_THRESHOLD, 1024 * 1024) == 1) {
        r.applied.push_back({true, "Malloc trim threshold lowered"});
    }
#endif
    return r;
}

OptimizationReport SystemOptimizer::setHighPriority() {
    OptimizationReport r;
#ifdef _WIN32
    if (SetPriorityClass(GetCurrentProcess(), HIGH_PRIORITY_CLASS)) {
        r.applied.push_back({true, "Process priority set to HIGH_PRIORITY_CLASS"});
    } else {
        r.failed.push_back({false, "SetPriorityClass() failed"});
    }

    if (SetThreadPriority(GetCurrentThread(), THREAD_PRIORITY_HIGHEST)) {
        r.applied.push_back({true, "Thread priority set to THREAD_PRIORITY_HIGHEST"});
    } else {
        r.failed.push_back({false, "SetThreadPriority() failed"});
    }

    
    r.applied.push_back({true, "I/O priority not adjustable on Windows (best-effort by default)"});
#else
    if (setpriority(PRIO_PROCESS, 0, -10) == 0) {
        r.applied.push_back({true, "CPU priority set to -10 (high)"});
    } else {
        r.failed.push_back({false, "setpriority() failed (try sudo)"});
    }

    int ioprio = (1 << 13) | 0;
    if (syscall(SYS_ioprio_set, 1, 0, ioprio) == 0) {
        r.applied.push_back({true, "I/O priority set to best-effort"});
    }
#endif
    return r;
}

OptimizationReport SystemOptimizer::adjustOOM() {
    OptimizationReport r;
#ifdef _WIN32
    r.applied.push_back({true, "OOM adjustment not applicable on Windows"});
#else
    std::ofstream oom("/proc/self/oom_score_adj");
    if (oom.is_open()) {
        oom << "-500";
        oom.close();
        r.applied.push_back({true, "OOM score adjusted to -500 (protected)"});
    } else {
        r.failed.push_back({false, "Could not adjust OOM score"});
    }
#endif
    return r;
}

OptimizationReport SystemOptimizer::cleanPageCache() {
    OptimizationReport r;
#ifdef _WIN32
    if (EmptyWorkingSet(GetCurrentProcess())) {
        r.applied.push_back({true, "Process working set emptied"});
    } else {
        r.failed.push_back({false, "EmptyWorkingSet() failed"});
    }
#else
    int fd = open("/proc/sys/vm/drop_caches", O_WRONLY);
    if (fd >= 0) {
        if (write(fd, "3", 1) == 1) {
            r.applied.push_back({true, "Kernel page cache dropped"});
        }
        close(fd);
    } else {
        r.failed.push_back({false, "drop_caches failed (not root?)"});
    }
#endif
    return r;
}

OptimizationReport SystemOptimizer::setCPUGovernor(const std::string& gov) {
    OptimizationReport r;
#ifdef _WIN32
    r.applied.push_back({true, "CPU governor not applicable on Windows"});
#else
    for (int cpu = 0; cpu < 128; cpu++) {
        std::string path = "/sys/devices/system/cpu/cpu" + std::to_string(cpu) + "/cpufreq/scaling_governor";
        std::ofstream f(path);
        if (!f.is_open()) break;
        f << gov;
        if (f.fail()) {
            r.failed.push_back({false, "Could not set governor on cpu" + std::to_string(cpu)});
        } else {
            r.applied.push_back({true, "CPU" + std::to_string(cpu) + " governor \u2192 " + gov});
        }
    }
    if (r.applied.empty()) {
        r.failed.push_back({false, "Could not set CPU governor (not root?)"});
    }
#endif
    return r;
}

OptimizationReport SystemOptimizer::killProcess(int pid) {
    OptimizationReport r;
    if (pid <= 0) {
        r.failed.push_back({false, "Invalid PID"});
        return r;
    }
#ifdef _WIN32
    HANDLE h = OpenProcess(PROCESS_TERMINATE, FALSE, (DWORD)pid);
    if (h) {
        if (TerminateProcess(h, 1)) {
            r.applied.push_back({true, "Terminated process PID " + std::to_string(pid)});
        } else {
            r.failed.push_back({false, "TerminateProcess() failed for PID " + std::to_string(pid)});
        }
        CloseHandle(h);
    } else {
        r.failed.push_back({false, "Could not open PID " + std::to_string(pid)});
    }
#else
    if (kill(pid, SIGTERM) == 0) {
        r.applied.push_back({true, "Sent SIGTERM to PID " + std::to_string(pid)});
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
        if (kill(pid, 0) == 0) {
            kill(pid, SIGKILL);
            r.applied.push_back({true, "Sent SIGKILL to PID " + std::to_string(pid)});
        }
    } else {
        r.failed.push_back({false, "Could not kill PID " + std::to_string(pid)});
    }
#endif
    return r;
}

OptimizationReport SystemOptimizer::suggestFreeMemory(uint64_t targetMB) {
    OptimizationReport r;
#ifdef _WIN32
    MEMORYSTATUSEX ms;
    ms.dwLength = sizeof(ms);
    if (GlobalMemoryStatusEx(&ms)) {
        uint64_t avail = ms.ullAvailPhys / (1024 * 1024);
        if (avail < targetMB) {
            r.applied.push_back({true,
                "Low memory: " + std::to_string(avail) + " MB available, "
                + std::to_string(targetMB) + " MB recommended. Consider closing browsers/IDEs."});
        }
    }
#else
    std::ifstream f("/proc/meminfo");
    if (!f.is_open()) return r;

    uint64_t avail = 0;
    std::string line;
    while (std::getline(f, line)) {
        if (line.find("MemAvailable:") == 0) {
            sscanf(line.c_str(), "MemAvailable: %lu kB", &avail);
            break;
        }
    }
    if (avail > 0 && avail / 1024 < targetMB) {
        r.applied.push_back({true,
            "Low memory: " + std::to_string(avail / 1024) + " MB available, "
            + std::to_string(targetMB) + " MB recommended. Consider closing browsers/IDEs."});
    }
#endif
    return r;
}

} 
} 
