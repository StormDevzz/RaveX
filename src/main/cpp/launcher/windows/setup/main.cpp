#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0A00
#endif
#include <windows.h>
#include <commctrl.h>
#include <shellapi.h>
#include <objbase.h>
#include <string>
#include "setup/include/setup_window.hpp"
bool isUninstallCmd() {
    int argc = 0;
    LPWSTR* argv = CommandLineToArgvW(GetCommandLineW(), &argc);
    if (!argv) return false;
    for (int i=1;i<argc;++i) {
        std::wstring a = argv[i];
        for (auto &c: a) c = towlower(c);
        if (a==L"/uninstall" || a==L"--uninstall" || a==L"/u" || a==L"uninstall") { LocalFree(argv); return true; }
    }
    LocalFree(argv);
    return false;
}

int APIENTRY WinMain(HINSTANCE hInstance, HINSTANCE, LPSTR, int) {
    CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    {
        // win10 1607+ only API, resolved dynamically for win7/8.1
        bool dpiSet = false;
        if (HMODULE u32 = GetModuleHandleW(L"user32.dll")) {
            using SetDpiAwarenessContextFn = BOOL(WINAPI*)(HANDLE);
            auto setCtx = reinterpret_cast<SetDpiAwarenessContextFn>(
                GetProcAddress(u32, "SetProcessDpiAwarenessContext"));
            if (setCtx) dpiSet = setCtx(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2) != FALSE;
        }
        if (!dpiSet) SetProcessDPIAware();
    }
    // The default install lives in %LOCALAPPDATA%\KickX, so setup must not ask
    // for elevation up front. Protected folders are rejected later with a
    // clear message, which keeps the common path fast and closable.
    // Windows 7/8.1 use the dynamically resolved DPI API above. Windows 10/11,
    // including future 26H2 builds, share the same version and compatibility
    // GUID, so no build-number gate is needed here.
    INITCOMMONCONTROLSEX icc{}; icc.dwSize = sizeof(icc); icc.dwICC = ICC_STANDARD_CLASSES; InitCommonControlsEx(&icc);
    if (isUninstallCmd()) {
        return ravex::setup::runUninstallWindow(hInstance) ? 0 : 1;
    }
    return ravex::setup::runSetupWindow(hInstance) ? 0 : 1;
}
