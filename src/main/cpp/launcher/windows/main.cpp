#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0A00
#endif

#include <windows.h>
#include <commctrl.h>
#include <fstream>
#include <string>
#include "core/include/config.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "ui/include/main_window.hpp"
#include "ui/include/splash_window.hpp"

namespace {
void logLauncher(const std::string& msg) {
    try {
        std::wstring dir = ravex::joinPath(ravex::kickxDir(), L"logs");
        ravex::createDirs(dir);
        std::wstring file = ravex::joinPath(dir, L"launcher.log");
        std::string narrow = ravex::toUtf8(file);
        std::ofstream ofs(narrow, std::ios::app);
        if (ofs) {
            SYSTEMTIME st; GetLocalTime(&st);
            char buf[64];
            std::snprintf(buf, sizeof(buf), "%04d-%02d-%02d %02d:%02d:%02d", st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond);
            ofs << "[" << buf << "] " << msg << "\n";
        }
    } catch (...) {}
}
LONG WINAPI crashHandler(EXCEPTION_POINTERS* ep) {
    try { logLauncher("CRASH code " + std::to_string(ep->ExceptionRecord->ExceptionCode)); } catch (...) {}
    return EXCEPTION_EXECUTE_HANDLER;
}
void logHardwareTelemetry() {
    try {
        ravex::LauncherConfig cfg = ravex::loadLauncherConfig();
        if (!cfg.telemetryEnabled) return;
        logLauncher("telemetry local only");
        SYSTEM_INFO si; GetSystemInfo(&si);
        logLauncher("cpu arch=" + std::to_string(si.wProcessorArchitecture) + " procs=" + std::to_string(si.dwNumberOfProcessors));
        MEMORYSTATUSEX ms; ms.dwLength = sizeof(ms); GlobalMemoryStatusEx(&ms);
        logLauncher("ram total=" + std::to_string(ms.ullTotalPhys / 1048576) + "MB avail=" + std::to_string(ms.ullAvailPhys / 1048576) + "MB");
        ULARGE_INTEGER freeB, totalB; std::wstring kdir = ravex::kickxDir();
        if (GetDiskFreeSpaceExW(kdir.c_str(), &freeB, &totalB, nullptr)) logLauncher("disk free=" + std::to_string(freeB.QuadPart / 1048576) + "MB total=" + std::to_string(totalB.QuadPart / 1048576) + "MB");
        OSVERSIONINFOEXW ovi; ZeroMemory(&ovi, sizeof(ovi)); ovi.dwOSVersionInfoSize = sizeof(ovi);
        HMODULE hMod = GetModuleHandleW(L"ntdll.dll");
        if (hMod) {
            typedef LONG(WINAPI * RtlGetVersionPtr)(OSVERSIONINFOEXW*);
            RtlGetVersionPtr p = (RtlGetVersionPtr)GetProcAddress(hMod, "RtlGetVersion");
            if (p) p(&ovi);
        }
        logLauncher("os " + std::to_string(ovi.dwMajorVersion) + "." + std::to_string(ovi.dwMinorVersion) + " build " + std::to_string(ovi.dwBuildNumber));
        DISPLAY_DEVICEW dd; ZeroMemory(&dd, sizeof(dd)); dd.cb = sizeof(dd);
        if (EnumDisplayDevicesW(nullptr, 0, &dd, 0)) logLauncher("gpu " + ravex::toUtf8(dd.DeviceString));
        logLauncher("java " + std::string(cfg.javaPath.empty() ? "bundled" : cfg.javaPath));
    } catch (...) {}
}
bool isWine() {
    HMODULE ntdll = GetModuleHandleW(L"ntdll.dll");
    if (ntdll && GetProcAddress(ntdll, "wine_get_version")) return true;
    if (ntdll && GetProcAddress(ntdll, "wine_get_host_version")) return true;
    if (ntdll && GetProcAddress(ntdll, "wine_server_call")) return true;
    HMODULE k32 = GetModuleHandleW(L"kernel32.dll");
    if (k32 && GetProcAddress(k32, "wine_get_unix_file_name")) return true;
    if (k32 && GetProcAddress(k32, "wine_get_dos_file_name")) return true;
    HKEY hKey;
    if (RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Wine", 0, KEY_READ, &hKey) == ERROR_SUCCESS) { RegCloseKey(hKey); return true; }
    if (RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"Software\\Wine", 0, KEY_READ, &hKey) == ERROR_SUCCESS) { RegCloseKey(hKey); return true; }
    if (GetEnvironmentVariableW(L"WINEPREFIX", nullptr, 0) > 0) return true;
    if (GetEnvironmentVariableW(L"WINELOADER", nullptr, 0) > 0) return true;
    if (GetEnvironmentVariableW(L"WINEDEBUG", nullptr, 0) > 0) return true;
    return false;
}
bool isProton() {
    if (GetEnvironmentVariableW(L"STEAM_COMPAT_DATA_PATH", nullptr, 0) > 0) return true;
    if (GetEnvironmentVariableW(L"STEAM_COMPAT_CLIENT_INSTALL_PATH", nullptr, 0) > 0) return true;
    if (GetEnvironmentVariableW(L"PROTON_VERSION", nullptr, 0) > 0) return true;
    if (GetEnvironmentVariableW(L"SteamGameId", nullptr, 0) > 0) return true;
    if (GetEnvironmentVariableW(L"SteamAppId", nullptr, 0) > 0) return true;
    if (GetEnvironmentVariableW(L"PRESSURE_VESSEL_FILESYSTEMS_RO", nullptr, 0) > 0) return true;
    if (GetEnvironmentVariableW(L"PRESSURE_VESSEL_FILESYSTEMS_RW", nullptr, 0) > 0) return true;
    if (GetFileAttributesW(L"Z:\\run\\pressure-vessel") != INVALID_FILE_ATTRIBUTES) return true;
    if (GetFileAttributesW(L"C:\\run\\pressure-vessel") != INVALID_FILE_ATTRIBUTES) return true;
    return false;
}
}

int APIENTRY WinMain(HINSTANCE hInstance, HINSTANCE, LPSTR, int) {
    SetUnhandledExceptionFilter(crashHandler);
    logLauncher("launcher started");
    logHardwareTelemetry();
    if (isWine() || isProton()) {
        logLauncher("blocked Wine/Proton launch");
        MessageBoxW(nullptr, L"Уже есть версия для Linux! Зачем ты пробуешь запустить Windows версию разбойник?", L"RaveX", MB_OK | MB_ICONWARNING);
        return 0;
    }
    if (!SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2)) {
        SetProcessDPIAware();
    }
    INITCOMMONCONTROLSEX icc{};
    icc.dwSize = sizeof(icc);
    icc.dwICC = ICC_STANDARD_CLASSES | ICC_WIN95_CLASSES | ICC_PROGRESS_CLASS;
    InitCommonControlsEx(&icc);
    ravex::ui::runSplashWindow(hInstance);
    return ravex::ui::runMainWindow(hInstance);
}