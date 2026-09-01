#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0A00
#endif
#include <windows.h>
#include <commctrl.h>
#include <shellapi.h>
#include <objbase.h>
#include <string>
#include "setup/include/setup_window.hpp"
bool isAdmin() {
    BOOL admin = FALSE;
    PSID adminGroup = nullptr;
    SID_IDENTIFIER_AUTHORITY ntAuth = SECURITY_NT_AUTHORITY;
    if (AllocateAndInitializeSid(&ntAuth, 2, SECURITY_BUILTIN_DOMAIN_RID, DOMAIN_ALIAS_RID_ADMINS, 0,0,0,0,0,0, &adminGroup)) {
        CheckTokenMembership(nullptr, adminGroup, &admin);
        FreeSid(adminGroup);
    }
    return admin == TRUE;
}
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
    if (!SetProcessDpiAwarenessContext(DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2)) SetProcessDPIAware();
    if (!isAdmin() && !isUninstallCmd()) {
        wchar_t exe[MAX_PATH]; GetModuleFileNameW(nullptr, exe, MAX_PATH);
        SHELLEXECUTEINFOW sei{}; sei.cbSize=sizeof(sei); sei.lpVerb=L"runas"; sei.lpFile=exe; sei.nShow=SW_SHOWNORMAL;
        if (ShellExecuteExW(&sei)) return 0;
    }
    INITCOMMONCONTROLSEX icc{}; icc.dwSize = sizeof(icc); icc.dwICC = ICC_STANDARD_CLASSES; InitCommonControlsEx(&icc);
    if (isUninstallCmd()) {
        return ravex::setup::runUninstallWindow(hInstance) ? 0 : 1;
    }
    return ravex::setup::runSetupWindow(hInstance) ? 0 : 1;
}
