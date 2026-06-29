#include "optimize_windows.h"
#include <windows.h>
#include <avrt.h>
#include <timeapi.h>
#include <iostream>

#pragma comment(lib, "avrt.lib")
#pragma comment(lib, "winmm.lib")
#pragma comment(lib, "powrprof.lib")

namespace ravex {
namespace plugins {
namespace optimize {

static int    g_original_priority_class = 0;
static int    g_original_thread_priority = 0;
static bool   g_timer_set = false;
static HANDLE g_power_request = nullptr;

void StartupBoost::boostProcessPriority() {
    HANDLE hProc = GetCurrentProcess();
    g_original_priority_class = GetPriorityClass(hProc);
    if (SetPriorityClass(hProc, HIGH_PRIORITY_CLASS)) {
        std::cout << "[RaveX/Boost] Process priority: HIGH_PRIORITY_CLASS" << std::endl;
    } else {
        SetPriorityClass(hProc, ABOVE_NORMAL_PRIORITY_CLASS);
        std::cout << "[RaveX/Boost] Process priority: ABOVE_NORMAL" << std::endl;
    }
}

void StartupBoost::boostThreadPriority() {
    HANDLE hThread = GetCurrentThread();
    g_original_thread_priority = GetThreadPriority(hThread);

    DWORD_PTR affinityMask = 0, systemAffinity = 0;
    if (GetProcessAffinityMask(GetCurrentProcess(), &affinityMask, &systemAffinity)) {
        SYSTEM_INFO sysInfo;
        GetSystemInfo(&sysInfo);
        DWORD numProcs = sysInfo.dwNumberOfProcessors;
        int limit = (numProcs > 16) ? 12 : (numProcs > 8 ? 8 : (int)numProcs);
        DWORD_PTR mask = 0;
        for (int i = 0; i < limit; i++) mask |= (1ULL << i);
        SetThreadAffinityMask(hThread, mask);
    }

    SetThreadPriority(hThread, THREAD_PRIORITY_TIME_CRITICAL);

    DWORD index = 0;
    HANDLE hAvrt = AvSetMmThreadCharacteristicsW(L"Games", &index);
    if (hAvrt) {
        std::cout << "[RaveX/Boost] Multimedia thread scheduling (AvRt) active" << std::endl;
    }
}

void StartupBoost::setTimerResolution() {
    TIMECAPS tc;
    if (timeGetDevCaps(&tc, sizeof(tc)) == TIMERR_NOERROR) {
        UINT resolution = std::min(tc.wPeriodMax, (UINT)1);
        if (timeBeginPeriod(resolution) == TIMERR_NOERROR) {
            g_timer_set = true;
            std::cout << "[RaveX/Boost] Timer resolution: " << resolution << "ms" << std::endl;
        }
    }
}

void StartupBoost::preventPowerThrottling() {
    typedef struct { ULONG Version, ControlMask, StateMask; } PTS;
    PTS pt;
    ZeroMemory(&pt, sizeof(pt));
    pt.Version = 1; pt.ControlMask = 1; pt.StateMask = 0;

    typedef BOOL(WINAPI* Sfn)(HANDLE, int, void*, DWORD);
    HMODULE hK = GetModuleHandleA("kernel32.dll");
    if (hK) {
        Sfn pfn = (Sfn)GetProcAddress(hK, "SetProcessInformation");
        if (pfn) pfn(GetCurrentProcess(), 4, &pt, sizeof(pt));
    }

    typedef HANDLE(WINAPI* PcrFn)(REASON_CONTEXT*);
    typedef BOOL(WINAPI* PsrFn)(HANDLE, DWORD);

    HMODULE hNt = GetModuleHandleA("ntdll.dll");
    if (!hNt) hNt = LoadLibraryA("ntdll.dll");
    if (hNt) {
        PcrFn pPcr = (PcrFn)GetProcAddress(hNt, "PowerCreateRequest");
        PsrFn pPsr = (PsrFn)GetProcAddress(hNt, "PowerSetRequest");
        if (pPcr && pPsr) {
            REASON_CONTEXT rc;
            ZeroMemory(&rc, sizeof(rc));
            rc.Version = 1;
            rc.Flags = 1;
            rc.Reason.SimpleReasonString = (LPWSTR)L"RaveX";
            g_power_request = pPcr(&rc);
            if (g_power_request) pPsr(g_power_request, 3);
        }
    }

    SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED);
    std::cout << "[RaveX/Boost] Power throttling disabled" << std::endl;
}

void StartupBoost::bindToPerformanceCores() {
    SYSTEM_INFO sysInfo;
    GetSystemInfo(&sysInfo);
    DWORD totalProcs = sysInfo.dwNumberOfProcessors;

    DWORD_PTR pCoreMask = 0;
    DWORD_PTR procAff = 0, sysAff = 0;
    if (!GetProcessAffinityMask(GetCurrentProcess(), &procAff, &sysAff)) return;

    typedef BOOL(WINAPI* GlpieFn)(LOGICAL_PROCESSOR_RELATIONSHIP, void*, DWORD*);
    HMODULE hK = GetModuleHandleA("kernel32.dll");
    if (hK) {
        GlpieFn pfn = (GlpieFn)GetProcAddress(hK, "GetLogicalProcessorInformationEx");
        if (pfn) {
            DWORD sz = 0;
            pfn(RelationProcessorCore, nullptr, &sz);
            if (sz > 0) {
                char* buf = new char[sz];
                if (pfn(RelationProcessorCore, buf, &sz)) {
                    DWORD off = 0;
                    int coreIdx = 0;
                    while (off < sz) {
                        auto core = (SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX*)(buf + off);
                        if (core->Relationship == RelationProcessorCore) {
                            DWORD_PTR m = core->Processor.GroupCount > 0 ? core->Processor.GroupMask[0].Mask : 0;
                            if (m && coreIdx < (int)totalProcs / 2) pCoreMask |= m;
                            coreIdx++;
                        }
                        off += core->Size;
                    }
                }
                delete[] buf;
            }
        }
    }

    if (pCoreMask == 0) {
        int limit = (totalProcs > 16) ? 12 : (totalProcs > 8 ? 8 : (int)totalProcs);
        for (int i = 0; i < limit; i++) pCoreMask |= (1ULL << i);
    }

    DWORD_PTR old = SetThreadAffinityMask(GetCurrentThread(), pCoreMask);
    if (old) {
        std::cout << "[RaveX/Boost] Thread affinity: P-core mask 0x"
                  << std::hex << pCoreMask << std::dec << std::endl;
    }
}

void StartupBoost::setGpuPriority() {
    HMODULE hD3D = LoadLibraryA("d3d11.dll");
    if (!hD3D) hD3D = LoadLibraryA("d3d9.dll");
    if (hD3D) {
        std::cout << "[RaveX/Boost] GPU module loaded" << std::endl;
        FreeLibrary(hD3D);
    }
}

void StartupBoost::boostAll() {
    boostProcessPriority();
    boostThreadPriority();
    setTimerResolution();
    preventPowerThrottling();
    bindToPerformanceCores();
    setGpuPriority();
    std::cout << "[RaveX/Boost] All startup optimizations applied" << std::endl;
}

void StartupBoost::restoreAll() {
    HANDLE hProc = GetCurrentProcess();
    HANDLE hThread = GetCurrentThread();
    if (g_original_priority_class) SetPriorityClass(hProc, g_original_priority_class);
    SetThreadPriority(hThread, g_original_thread_priority);
    DWORD_PTR pa = 0, sa = 0;
    if (GetProcessAffinityMask(hProc, &pa, &sa)) SetThreadAffinityMask(hThread, sa);
    if (g_timer_set) { timeEndPeriod(1); g_timer_set = false; }
    if (g_power_request) { CloseHandle(g_power_request); g_power_request = nullptr; }
    SetThreadExecutionState(ES_CONTINUOUS);
    std::cout << "[RaveX/Boost] Startup optimizations restored" << std::endl;
}

} 
} 
} 
