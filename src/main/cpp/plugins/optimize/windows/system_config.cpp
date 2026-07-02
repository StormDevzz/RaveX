#include "optimize_windows.hpp"
#include <winsock2.h>
#include <windows.h>
#include <powrprof.h>
#include <iostream>
#include <sstream>
#include <string>

#pragma comment(lib, "powrprof.lib")
#pragma comment(lib, "ws2_32.lib")

namespace ravex {
namespace plugins {
namespace optimize {

bool SystemConfig::isHighPerformancePowerPlan() {
    GUID* activePlan = nullptr;
    if (PowerGetActiveScheme(NULL, &activePlan) != ERROR_SUCCESS)
        return false;

    GUID hp = {0x8c5e7fda, 0xe8bf, 0x4a96, {0x9a, 0x85, 0xa6, 0xe2, 0x3a, 0x8c, 0x63, 0x5c}};
    bool ok = (memcmp(activePlan, &hp, sizeof(GUID)) == 0);
    std::cout << "[RaveX/System] Power plan: " << (ok ? "High Performance" : "Balanced") << std::endl;
    LocalFree(activePlan);
    return ok;
}

bool SystemConfig::isGameModeEnabled() {
    HKEY hKey;
    DWORD val = 0, sz = sizeof(val);
    if (RegOpenKeyExA(HKEY_CURRENT_USER, "Software\\Microsoft\\GameBar", 0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        if (RegQueryValueExA(hKey, "AllowAutoGameMode", NULL, NULL, (LPBYTE)&val, &sz) == ERROR_SUCCESS) {
            RegCloseKey(hKey);
            std::cout << "[RaveX/System] Game Mode: " << (val ? "enabled" : "disabled") << std::endl;
            return val != 0;
        }
        RegCloseKey(hKey);
    }
    std::cout << "[RaveX/System] Game Mode: unknown" << std::endl;
    return false;
}

bool SystemConfig::isCoreIsolationEnabled() {
    HKEY hKey;
    DWORD val = 0, sz = sizeof(val);
    if (RegOpenKeyExA(HKEY_LOCAL_MACHINE,
            "SYSTEM\\CurrentControlSet\\Control\\DeviceGuard\\Scenarios\\HypervisorEnforcedCodeIntegrity",
            0, KEY_READ, &hKey) == ERROR_SUCCESS) {
        if (RegQueryValueExA(hKey, "Enabled", NULL, NULL, (LPBYTE)&val, &sz) == ERROR_SUCCESS) {
            RegCloseKey(hKey);
            std::cout << "[RaveX/System] HVCI: " << (val ? "enabled" : "disabled") << std::endl;
            return val != 0;
        }
        RegCloseKey(hKey);
    }
    std::cout << "[RaveX/System] HVCI: unknown" << std::endl;
    return false;
}

bool SystemConfig::isRunningOnSsd() {
    wchar_t path[MAX_PATH];
    if (!GetModuleFileNameW(NULL, path, MAX_PATH)) return false;

    std::wstring drive = std::wstring(1, path[0]) + L":\\";
    bool ssd = false;

    HANDLE hD = CreateFileW(drive.c_str(), 0, FILE_SHARE_READ|FILE_SHARE_WRITE,
                            NULL, OPEN_EXISTING, 0, NULL);
    if (hD != INVALID_HANDLE_VALUE) {
        DWORD ret = 0;
        STORAGE_PROPERTY_QUERY q;
        ZeroMemory(&q, sizeof(q));
        q.PropertyId = StorageDeviceSeekPenaltyProperty;
        q.QueryType = PropertyStandardQuery;
        DEVICE_SEEK_PENALTY_DESCRIPTOR d;
        ZeroMemory(&d, sizeof(d));
        if (DeviceIoControl(hD, IOCTL_STORAGE_QUERY_PROPERTY,
                &q, sizeof(q), &d, sizeof(d), &ret, NULL)) {
            ssd = (d.IncursSeekPenalty == 0);
        }
        CloseHandle(hD);
    }

    std::cout << "[RaveX/System] Drive " << (char)path[0] << ":\\ "
              << (ssd ? "SSD" : "HDD") << std::endl;
    return ssd;
}

void SystemConfig::disableNagle() {
    HMODULE hWI = LoadLibraryA("wininet.dll");
    if (hWI) {
        typedef BOOL(WINAPI* IsoFn)(HANDLE, DWORD, void*, DWORD);
        IsoFn pfn = (IsoFn)GetProcAddress(hWI, "InternetSetOptionA");
        if (pfn) { DWORD v = 1; pfn(NULL, 78, &v, sizeof(v)); }
        FreeLibrary(hWI);
    }

    WSADATA wd;
    if (WSAStartup(MAKEWORD(2,2), &wd) == 0) {
        SOCKET s = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        if (s != INVALID_SOCKET) {
            int nd = 1;
            setsockopt(s, IPPROTO_TCP, TCP_NODELAY, (char*)&nd, sizeof(nd));
            closesocket(s);
        }
        WSACleanup();
    }
    std::cout << "[RaveX/System] Nagle disabled for TCP" << std::endl;
}

std::string SystemConfig::getSystemInfo() {
    std::ostringstream oss;
    SYSTEM_INFO si;
    GetSystemInfo(&si);
    oss << "CPU: " << si.dwNumberOfProcessors << " cores";

    MEMORYSTATUSEX ms;
    ms.dwLength = sizeof(ms);
    if (GlobalMemoryStatusEx(&ms))
        oss << " | RAM: " << (ms.ullTotalPhys / 1024/1024/1024) << "GB ("
            << ms.dwMemoryLoad << "% used)";

    typedef LONG(WINAPI* RgvFn)(PRTL_OSVERSIONINFOEXW);
    HMODULE hNt = GetModuleHandleW(L"ntdll.dll");
    if (hNt) {
        RgvFn pfn = (RgvFn)GetProcAddress(hNt, "RtlGetVersion");
        if (pfn) {
            RTL_OSVERSIONINFOEXW oi;
            ZeroMemory(&oi, sizeof(oi));
            oi.dwOSVersionInfoSize = sizeof(oi);
            if (pfn(&oi) == 0)
                oss << " | Win " << oi.dwMajorVersion << "." << oi.dwMinorVersion
                    << " build " << oi.dwBuildNumber;
        }
    }

    return oss.str();
}

bool SystemConfig::checkAll() {
    isHighPerformancePowerPlan();
    isGameModeEnabled();
    isCoreIsolationEnabled();
    isRunningOnSsd();
    disableNagle();
    std::cout << "[RaveX/System] " << getSystemInfo() << std::endl;
    std::cout << "[RaveX/System] All system checks done" << std::endl;
    return true;
}

} 
} 
} 
