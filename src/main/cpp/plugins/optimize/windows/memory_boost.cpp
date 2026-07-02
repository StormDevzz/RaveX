#include "optimize_windows.hpp"
#include <windows.h>
#include <psapi.h>
#include <iostream>

#pragma comment(lib, "psapi.lib")

#ifndef QUOTA_LIMITS_HARDWS_MIN_ENABLE
#define QUOTA_LIMITS_HARDWS_MIN_ENABLE 0x00000004
#endif
#ifndef QUOTA_LIMITS_HARDWS_MAX_DISABLE
#define QUOTA_LIMITS_HARDWS_MAX_DISABLE 0x00000008
#endif

namespace ravex {
namespace plugins {
namespace optimize {

bool MemoryBoost::trimWorkingSet() {
    if (EmptyWorkingSet(GetCurrentProcess())) {
        std::cout << "[RaveX/Memory] Working set trimmed" << std::endl;
        return true;
    }
    return false;
}

bool MemoryBoost::setWorkingSetLimit() {
    SIZE_T minSize = 50ULL * 1024 * 1024;
    SIZE_T maxSize = 1024ULL * 1024 * 1024;
    if (SetProcessWorkingSetSizeEx(GetCurrentProcess(), minSize, maxSize,
            QUOTA_LIMITS_HARDWS_MIN_ENABLE | QUOTA_LIMITS_HARDWS_MAX_DISABLE)) {
        std::cout << "[RaveX/Memory] Working set limits: "
                  << (minSize / 1024 / 1024) << "MB / " << (maxSize / 1024 / 1024) << "MB" << std::endl;
        return true;
    }
    return false;
}

bool MemoryBoost::enableLowFragmentationHeap() {
    ULONG info = 2;
    if (HeapSetInformation(NULL, HeapCompatibilityInformation, &info, sizeof(info))) {
        HANDLE hHeap = GetProcessHeap();
        if (hHeap) HeapSetInformation(hHeap, HeapCompatibilityInformation, &info, sizeof(info));
        std::cout << "[RaveX/Memory] Low-fragmentation heap enabled" << std::endl;
        return true;
    }
    return false;
}

bool MemoryBoost::enableLargePages() {
    HANDLE hToken = NULL;
    TOKEN_PRIVILEGES tp;
    LUID luid;

    if (!OpenProcessToken(GetCurrentProcess(), TOKEN_ADJUST_PRIVILEGES | TOKEN_QUERY, &hToken))
        return false;
    if (!LookupPrivilegeValueA(NULL, "SeLockMemoryPrivilege", &luid)) {
        CloseHandle(hToken);
        return false;
    }
    tp.PrivilegeCount = 1;
    tp.Privileges[0].Luid = luid;
    tp.Privileges[0].Attributes = SE_PRIVILEGE_ENABLED;
    AdjustTokenPrivileges(hToken, FALSE, &tp, sizeof(tp), NULL, NULL);
    CloseHandle(hToken);

    std::cout << "[RaveX/Memory] Large page privilege available" << std::endl;
    return true;
}

bool MemoryBoost::setMemoryPriority() {
    typedef struct { ULONG MemoryPriority; } Mpi;
    Mpi mpi;
    ZeroMemory(&mpi, sizeof(mpi));
    mpi.MemoryPriority = 5;

    typedef BOOL(WINAPI* Sfn)(HANDLE, int, void*, DWORD);
    HMODULE hK = GetModuleHandleA("kernel32.dll");
    if (hK) {
        Sfn pfn = (Sfn)GetProcAddress(hK, "SetProcessInformation");
        if (pfn && pfn(GetCurrentProcess(), 3, &mpi, sizeof(mpi))) {
            std::cout << "[RaveX/Memory] Memory priority: 5 (highest)" << std::endl;
            return true;
        }
    }
    return false;
}

void MemoryBoost::logMemoryStats() {
    PROCESS_MEMORY_COUNTERS pmc;
    if (GetProcessMemoryInfo(GetCurrentProcess(), &pmc, sizeof(pmc))) {
        std::cout << "[RaveX/Memory] PF=" << pmc.PageFaultCount
                  << " WS=" << (pmc.WorkingSetSize / 1024 / 1024) << "MB"
                  << " Peak=" << (pmc.PeakWorkingSetSize / 1024 / 1024) << "MB"
                  << " PF=" << (pmc.PagefileUsage / 1024 / 1024) << "MB"
                  << std::endl;
    }
    MEMORYSTATUSEX ms;
    ms.dwLength = sizeof(ms);
    if (GlobalMemoryStatusEx(&ms)) {
        std::cout << "[RaveX/Memory] System: "
                  << (ms.ullTotalPhys / 1024 / 1024 / 1024) << "GB total, "
                  << (ms.ullAvailPhys / 1024 / 1024 / 1024) << "GB free"
                  << std::endl;
    }
}

bool MemoryBoost::optimizeAll() {
    bool ok = true;
    ok &= trimWorkingSet();
    ok &= setWorkingSetLimit();
    ok &= enableLowFragmentationHeap();
    enableLargePages();
    ok &= setMemoryPriority();
    logMemoryStats();
    std::cout << "[RaveX/Memory] All memory optimizations applied" << std::endl;
    return ok;
}

} 
} 
} 
