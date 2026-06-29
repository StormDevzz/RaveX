#include "optimizer.h"
#include <iostream>
#include <thread>
#include <chrono>
#include <vector>
#include <cstring>
#ifndef _WIN32
#include <unistd.h>
#include <malloc.h>
#include <sched.h>
#include <sys/mman.h>
#else
#include <windows.h>
#include <malloc.h>
#endif

namespace ravex {

std::vector<std::string> Optimizer::listTechniques() {
    return {
        "malloc_trim (glibc heap vacuum)",
        "madvise MADV_DONTNEED (free page hints)",
        "madvise MADV_COLD (cold page hints)",
        "madvise MADV_PAGEOUT (swap cold pages)",
        "Process priority (nice -20..0)",
        "I/O priority (idle class)",
        "OOM score adjustment (-1000..0)",
        "CPU scheduler (SCHED_FIFO/SCHED_RR)",
        "Thread affinity pinning",
        "drop_caches (kernel page cache)",
        "compact_memory (kernel defrag)"
    };
}

OptResult Optimizer::run(const std::string& mode) {
    if (mode == "Aggressive" || mode == "aggressive") return aggressive();
    if (mode == "Normal" || mode == "normal")         return normal();
    if (mode == "Soft" || mode == "soft")             return soft();
    return {false, "unknown mode", 0, 0, 0};
}

void Optimizer::vacuumGlibc() {
#ifdef _WIN32
    for (int i = 0; i < 5; i++) {
        _heapmin();
        SetProcessWorkingSetSize(GetCurrentProcess(), (SIZE_T)-1, (SIZE_T)-1);
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
#else
    for (int i = 0; i < 5; i++) {
        malloc_trim(0);
        std::this_thread::sleep_for(std::chrono::milliseconds(5));
    }
#endif
}

bool Optimizer::hintHeapPages() {
#ifndef _WIN32
    extern char __heap_start, __heap_end;
    void* heap_end = sbrk(0);
    if (heap_end == (void*)-1) return false;
    size_t heap_size = reinterpret_cast<size_t>(heap_end);
    if (heap_size > 1024 * 1024) {
        madvise(heap_end, 4096, MADV_COLD);
    }
    return true;
#else
    return false;
#endif
}

uint64_t Optimizer::measureFreeDelta(uint64_t before) {
    auto after = Memory::readMemInfo();
    return after.free_kb > before ? after.free_kb - before : 0;
}

OptResult Optimizer::aggressive() {
    int actions = 0;
    auto before = Memory::readMemInfo();

    Memory::setProcessPriority(-20);
    actions++;

    Memory::setIONice(3, 0);
    actions++;

    Memory::setOOMScoreAdj(-1000);
    actions++;

    Memory::setThreadAffinity(0);
    actions++;

    Memory::setCPUSchedPolicy(
#ifdef _WIN32
        0
#else
        SCHED_FIFO
#endif
    , 50);
    actions++;

    vacuumGlibc();
    actions++;

    Memory::dropCaches();
    actions++;

    Memory::compactMemory();
    actions++;

    hintHeapPages();
    actions++;

    std::this_thread::sleep_for(std::chrono::milliseconds(100));
    auto after = Memory::readMemInfo();
    uint64_t freed = after.free_kb > before.free_kb ? after.free_kb - before.free_kb : 0;

    std::string msg = "Aggressive: "
        + std::to_string(actions) + " techniques applied, "
        + "freed ~" + std::to_string(freed) + " KB";
    return {true, msg, after.free_kb, freed, actions};
}

OptResult Optimizer::normal() {
    int actions = 0;
    auto before = Memory::readMemInfo();

    Memory::setProcessPriority(-15);
    actions++;

    Memory::setIONice(3, 0);
    actions++;

    Memory::setOOMScoreAdj(-500);
    actions++;

    Memory::setThreadAffinity(0);
    actions++;

    for (int i = 0; i < 3; i++) {
#ifdef _WIN32
        _heapmin();
#else
        malloc_trim(0);
#endif
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }
    actions++;

    auto after = Memory::readMemInfo();
    uint64_t freed = after.free_kb > before.free_kb ? after.free_kb - before.free_kb : 0;

    std::string msg = "Normal: "
        + std::to_string(actions) + " techniques applied, "
        + "freed ~" + std::to_string(freed) + " KB";
    return {true, msg, after.free_kb, freed, actions};
}

OptResult Optimizer::soft() {
    auto before = Memory::readMemInfo();
    double usedPct = 100.0 * (1.0 - (double)before.free_kb / (double)before.total_kb);

    if (usedPct > 65.0) {
#ifdef _WIN32
        _heapmin();
#else
        malloc_trim(0);
#endif
        Memory::setOOMScoreAdj(-200);
    }

    auto after = Memory::readMemInfo();
    uint64_t freed = after.free_kb > before.free_kb ? after.free_kb - before.free_kb : 0;

    std::string msg = usedPct > 65.0
        ? "Soft: memory usage was " + std::to_string((int)usedPct) + "%, trimmed"
        : "Soft: memory usage " + std::to_string((int)usedPct) + "%, no action needed";
    return {true, msg, after.free_kb, freed, usedPct > 65.0 ? 1 : 0};
}

} 
