#include "setup/include/setup_window.hpp"
#include "core/include/config.hpp"
#include "core/include/paths.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "core/include/lang.hpp"
#include "game/include/mojang.hpp"
#include "game/include/java.hpp"
#include "ui/include/glow.hpp"
#include <windows.h>
#include <commctrl.h>
#include <uxtheme.h>
#include <gdiplus.h>
#include <shlobj.h>
#include <shobjidl.h>
#include <string>
#include <thread>
namespace ravex::setup {
namespace {
constexpr int IDC_STATUS = 2001;
constexpr int IDC_PROGRESS = 2002;
constexpr int IDC_INSTALL = 2003;
constexpr int IDC_PATH = 2005;
constexpr int IDC_BROWSE = 2006;
constexpr int IDC_SHORTCUT_DESKTOP = 2007;
constexpr int IDC_SHORTCUT_START = 2008;
constexpr int IDC_NEXT = 2009;
constexpr int IDC_BACK = 2010;
constexpr int IDC_STARTMENU = 2012;
constexpr int IDC_DETAILS = 2013;
constexpr int IDC_LANG = 2014;
struct SetupData {
    HWND hwnd = nullptr;
    HWND hTitle = nullptr;
    HWND hStatus = nullptr;
    HWND hProgress = nullptr;
    HWND hDetailsBtn = nullptr;
    HWND hDetailsEdit = nullptr;
    HWND hInstall = nullptr;
    HWND hNext = nullptr;
    HWND hBack = nullptr;
    HWND hPath = nullptr;
    HWND hBrowse = nullptr;
    HWND hShortcutDesktop = nullptr;
    HWND hShortcutStart = nullptr;
    HWND hStartMenu = nullptr;
    HWND hWelcome = nullptr;
    HWND hSummary = nullptr;
    HWND hLangCombo = nullptr;
    HWND hLangLabel = nullptr;
    HFONT font = nullptr;
    HFONT titleFont = nullptr;
    HBRUSH bgBrush = nullptr;
    ThemeColors theme;
    ravex::ui::GlowData glow{};
    bool busy = false;
    bool desktopChecked = true;
    bool startChecked = true;
    bool detailsVisible = false;
    float spinnerPhase = 0;
    float fadeAlpha = 1.0f;
    int fadeDir = 0;
    int slideOffset = 0;
    std::wstring detailsLog;
    int page = 0;
    int nextPage = 0;
};
SetupData g_setup;
HFONT makeSetupFont() {
    HFONT f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    if (!f) f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    return f;
}
HFONT makeTitleFont() {
    HFONT f = CreateFontW(-19, 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    if (!f) f = CreateFontW(-19, 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    return f;
}
void updateLangTexts() {
    SetWindowTextW(g_setup.hTitle, fromUtf8(lang("setup_title")).c_str());
    std::wstring welcome = fromUtf8(std::string(lang("setup_welcome")) + "\r\n" + lang("setup_welcome2"));
    SetWindowTextW(g_setup.hWelcome, welcome.c_str());
    SetWindowTextW(g_setup.hLangLabel, fromUtf8(lang("setup_lang")).c_str());
    SetWindowTextW(GetDlgItem(g_setup.hwnd, 3001), fromUtf8(lang("setup_dest")).c_str());
    SetWindowTextW(GetDlgItem(g_setup.hwnd, 3002), fromUtf8(lang("setup_start")).c_str());
    SetWindowTextW(g_setup.hShortcutDesktop, fromUtf8(lang("setup_shortcut_desktop")).c_str());
    SetWindowTextW(g_setup.hShortcutStart, fromUtf8(lang("setup_shortcut_start")).c_str());
    SetWindowTextW(g_setup.hNext, fromUtf8(lang("setup_next")).c_str());
    SetWindowTextW(g_setup.hBack, fromUtf8(lang("setup_back")).c_str());
    SetWindowTextW(g_setup.hInstall, fromUtf8(lang("setup_install")).c_str());
    SetWindowTextW(g_setup.hBrowse, fromUtf8(lang("setup_browse")).c_str());
    SetWindowTextW(g_setup.hDetailsBtn, fromUtf8(std::string(lang("setup_details")) + "  \xE2\x96\xBC").c_str());
}
void setStatus(const std::string& s) {
    SetWindowTextW(g_setup.hStatus, fromUtf8(s).c_str());
}
void appendDetail(const std::string& s) {
    g_setup.detailsLog += fromUtf8(s) + L"\r\n";
    if (g_setup.hDetailsEdit) {
        SetWindowTextW(g_setup.hDetailsEdit, g_setup.detailsLog.c_str());
        SendMessageW(g_setup.hDetailsEdit, EM_SETSEL, (WPARAM)g_setup.detailsLog.size(), (LPARAM)g_setup.detailsLog.size());
        SendMessageW(g_setup.hDetailsEdit, EM_SCROLLCARET, 0, 0);
    }
}
void setStatusDetail(const std::string& s) {
    setStatus(s);
    appendDetail(s);
}
void drawSpinner(HDC hdc, int cx, int cy, float phase) {
    Gdiplus::Graphics g(hdc);
    g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
    const int dots = 12; const int R = 26; const float step = 6.2831853f / dots;
    for (int i = 0; i < dots; ++i) {
        float a = step * i + phase;
        float px = cx + cosf(a) * R;
        float py = cy + sinf(a) * R;
        float fade = 1.0f - (float)i / dots; fade = fade*fade;
        BYTE alpha = (BYTE)(90 + fade*165);
        Gdiplus::SolidBrush br(Gdiplus::Color(alpha, 0, 120, 212));
        g.FillEllipse(&br, px-4.0f, py-4.0f, 8.0f, 8.0f);
    }
}
void showPageImmediate(int page) {
    g_setup.page = page;
    ShowWindow(g_setup.hLangCombo, page == 0 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hLangLabel, page == 0 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hWelcome, page == 1 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hPath, page == 2 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hBrowse, page == 2 ? SW_SHOW : SW_HIDE);
    ShowWindow(GetDlgItem(g_setup.hwnd, 3001), page == 2 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hStartMenu, page == 3 ? SW_SHOW : SW_HIDE);
    ShowWindow(GetDlgItem(g_setup.hwnd, 3002), page == 3 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hShortcutDesktop, page == 4 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hShortcutStart, page == 4 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hSummary, page == 5 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hStatus, page == 6 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hProgress, SW_HIDE);
    ShowWindow(g_setup.hDetailsBtn, page == 6 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hDetailsEdit, (page == 6 && g_setup.detailsVisible) ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hInstall, page == 5 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hNext, (page >= 0 && page < 5) ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hBack, (page > 0 && page < 6) ? SW_SHOW : SW_HIDE);
    if (page == 5) {
        wchar_t path[MAX_PATH]; GetWindowTextW(g_setup.hPath, path, MAX_PATH);
        wchar_t menu[MAX_PATH]; GetWindowTextW(g_setup.hStartMenu, menu, MAX_PATH);
        std::wstring sum = fromUtf8(lang("setup_summary_to")) + std::wstring(path) + L"\r\n" + fromUtf8(lang("setup_summary_menu")) + std::wstring(menu);
        SetWindowTextW(g_setup.hSummary, sum.c_str());
    }
    if (page == 6) SetTimer(g_setup.hwnd, 50, 16, nullptr); else KillTimer(g_setup.hwnd, 50);
    EnableWindow(g_setup.hNext, TRUE);
    InvalidateRect(g_setup.hwnd, nullptr, TRUE);
}
void doFadeTo(int target) {
    if (target == g_setup.page) return;
    g_setup.slideOffset = -28;
    showPageImmediate(target);
    RECT rc; GetClientRect(g_setup.hwnd, &rc);
    int w = rc.right - rc.left;
    HDWP dwp = BeginDeferWindowPos(10);
    if (dwp) {
        auto placeCard = [&](HWND hw, int x, int y, int cw, int ch){ DeferWindowPos(dwp, hw, nullptr, x, y-28, cw, ch, SWP_NOZORDER | SWP_NOOWNERZORDER | SWP_NOCOPYBITS); };
        placeCard(g_setup.hLangCombo, 20, 74, 300, 200);
        placeCard(g_setup.hLangLabel, 20, 50, w-40, 22);
        placeCard(g_setup.hPath, 20, 80, w - 140, 24);
        placeCard(GetDlgItem(g_setup.hwnd, 3001), 20, 55, w-40, 22);
        placeCard(g_setup.hStartMenu, 20, 80, w-40, 24);
        placeCard(GetDlgItem(g_setup.hwnd, 3002), 20, 55, w-40, 22);
        placeCard(g_setup.hShortcutDesktop, 20, 65, 260, 28);
        placeCard(g_setup.hShortcutStart, 20, 100, 280, 28);
        placeCard(g_setup.hStatus, 0, 98, w, 16);
        placeCard(g_setup.hDetailsBtn, 20, 120, 90, 22);
        placeCard(g_setup.hSummary, 20, 55, w-40, 60);
        placeCard(g_setup.hWelcome, 20, 55, w-40, 40);
        EndDeferWindowPos(dwp);
    }
    SetTimer(g_setup.hwnd, 51, 16, nullptr);
}
void showPage(int page) { doFadeTo(page); }
void createShortcut(const std::wstring& target, const std::wstring& linkPath, const std::wstring& desc) {
    CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    IShellLinkW* psl = nullptr;
    if (SUCCEEDED(CoCreateInstance(CLSID_ShellLink, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&psl)))) {
        psl->SetPath(target.c_str());
        psl->SetDescription(desc.c_str());
        psl->SetWorkingDirectory(joinPath(target.substr(0, target.find_last_of(L"\\/")), L"").c_str());
        psl->SetIconLocation(target.c_str(), 0);
        IPersistFile* ppf = nullptr;
        if (SUCCEEDED(psl->QueryInterface(IID_PPV_ARGS(&ppf)))) {
            ppf->Save(linkPath.c_str(), TRUE);
            ppf->Release();
        }
        psl->Release();
    }
}
bool isValidLauncherBinary(const std::wstring& path) {
    if (!fileExists(path)) return false;
    HANDLE h = CreateFileW(path.c_str(), GENERIC_READ, FILE_SHARE_READ, nullptr, OPEN_EXISTING, 0, nullptr);
    if (h == INVALID_HANDLE_VALUE) return false;
    BYTE hdr[2] = {0};
    DWORD r = 0;
    bool ok = ReadFile(h, hdr, 2, &r, nullptr) && r == 2 && hdr[0] == 'M' && hdr[1] == 'Z';
    LARGE_INTEGER sz{};
    if (ok && GetFileSizeEx(h, &sz) && sz.QuadPart < 1024 * 100) ok = false;
    CloseHandle(h);
    if (!ok) return false;
    std::string narrow = toUtf8(path);
    if (narrow.find("KickXSetup") != std::string::npos) return false;
    return true;
}
bool extractEmbeddedLauncher(const std::wstring& dst) {
    HMODULE hMod = GetModuleHandleW(nullptr);
    HRSRC hRes = FindResourceW(hMod, MAKEINTRESOURCEW(5000), MAKEINTRESOURCEW(10));
    if (!hRes) hRes = FindResourceW(nullptr, MAKEINTRESOURCEW(5000), MAKEINTRESOURCEW(10));
    if (!hRes) return false;
    HGLOBAL hData = LoadResource(hMod ? hMod : GetModuleHandleW(nullptr), hRes);
    if (!hData) return false;
    void* data = LockResource(hData);
    DWORD size = SizeofResource(hMod ? hMod : GetModuleHandleW(nullptr), hRes);
    if (!data || size < 1024 * 100) return false;
    if (size >= 2 && static_cast<BYTE*>(data)[0] != 'M' && static_cast<BYTE*>(data)[1] != 'Z') return false;
    HANDLE hFile = CreateFileW(dst.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (hFile == INVALID_HANDLE_VALUE) return false;
    DWORD written = 0;
    bool ok = WriteFile(hFile, data, size, &written, nullptr) && written == size;
    CloseHandle(hFile);
    if (!ok) { DeleteFileW(dst.c_str()); return false; }
    return isValidLauncherBinary(dst);
}
void workerSetup() {
    g_setup.busy = true;
    g_setup.detailsLog.clear();
    if (g_setup.hDetailsEdit) SetWindowTextW(g_setup.hDetailsEdit, L"");
    EnableWindow(g_setup.hInstall, FALSE);
    wchar_t pathBuf[MAX_PATH]; GetWindowTextW(g_setup.hPath, pathBuf, MAX_PATH);
    std::wstring installDir = pathBuf;
    if (installDir.empty()) { setStatusDetail(lang("setup_select_folder")); EnableWindow(g_setup.hInstall, TRUE); g_setup.busy = false; return; }
    createDirs(installDir);
    wchar_t exePath[MAX_PATH]; GetModuleFileNameW(nullptr, exePath, MAX_PATH);
    std::wstring srcDir = exePath; size_t p = srcDir.find_last_of(L"\\/"); if (p != std::wstring::npos) srcDir = srcDir.substr(0, p);
    std::wstring dstLauncher = joinPath(installDir, L"kickx_launcher.exe");
    std::wstring srcLauncher = joinPath(srcDir, L"kickx_launcher.exe");
    bool launcherCopied = false;
    if (fileExists(srcLauncher)) {
        launcherCopied = CopyFileW(srcLauncher.c_str(), dstLauncher.c_str(), FALSE) != 0;
        if (launcherCopied && isValidLauncherBinary(dstLauncher)) appendDetail("Launcher copied from package: " + toUtf8(srcLauncher));
        else if (launcherCopied) { DeleteFileW(dstLauncher.c_str()); launcherCopied = false; }
    }
    if (!launcherCopied || !isValidLauncherBinary(dstLauncher)) {
        appendDetail("Side-by-side launcher not found, trying embedded resource...");
        if (extractEmbeddedLauncher(dstLauncher)) {
            launcherCopied = true;
            appendDetail("Launcher extracted from embedded resource (" + std::to_string(GetFileAttributesW(dstLauncher.c_str()) != INVALID_FILE_ATTRIBUTES ? 1 : 0) + ")");
        }
    }
    if (!launcherCopied || !isValidLauncherBinary(dstLauncher)) {
        if (launcherCopied) DeleteFileW(dstLauncher.c_str());
        setStatusDetail(std::string(lang("setup_launcher_missing")) + " " + toUtf8(srcLauncher));
        appendDetail("ERROR: kickx_launcher.exe not found next to installer. Expected at: " + toUtf8(srcLauncher));
        appendDetail("Please extract the ZIP fully (both KickXSetup.exe and kickx_launcher.exe) and run setup from extracted folder.");
        MessageBoxW(g_setup.hwnd, fromUtf8(std::string(lang("setup_launcher_missing")) + "\n" + toUtf8(srcLauncher) + "\n\n" + lang("setup_extract_hint")).c_str(), fromUtf8(lang("setup_title")).c_str(), MB_OK | MB_ICONERROR);
        EnableWindow(g_setup.hInstall, TRUE);
        g_setup.busy = false;
        return;
    }
    std::wstring dstSetup = joinPath(installDir, L"KickXSetup.exe");
    CopyFileW(exePath, dstSetup.c_str(), FALSE);
    std::wstring srcIcons = joinPath(srcDir, L"icons");
    std::wstring dstIcons = joinPath(installDir, L"icons");
    if (fileExists(srcIcons)) { createDirs(dstIcons); }
    SHFILEOPSTRUCTW op{}; op.wFunc = FO_COPY; std::wstring from = srcIcons + L"\\*\0"; std::wstring to = dstIcons + L"\0"; from.push_back(L'\0'); to.push_back(L'\0'); op.pFrom = from.c_str(); op.pTo = to.c_str(); op.fFlags = FOF_NOCONFIRMATION | FOF_NOERRORUI; SHFileOperationW(&op);
    std::wstring srcFlags = joinPath(srcDir, L"flags");
    std::wstring dstFlags = joinPath(installDir, L"flags");
    if (fileExists(srcFlags)) { createDirs(dstFlags); std::wstring from2 = srcFlags + L"\\*\0"; std::wstring to2 = dstFlags + L"\0"; from2.push_back(L'\0'); to2.push_back(L'\0'); op.pFrom = from2.c_str(); op.pTo = to2.c_str(); SHFileOperationW(&op); }
    std::string err;
    std::string version = "1.21.11";
    auto postProgress = [](const ravex::net::Progress& p) {
        if (!p.url.empty()) {
            size_t pos = p.url.find_last_of("/\\");
            std::string name = (pos == std::string::npos) ? p.url : p.url.substr(pos+1);
            if (p.total > 0) {
                int pct = int(p.downloaded * 100 / p.total);
                std::string msg = std::string(lang("setup_downloading")) + " " + name + " (" + std::to_string(pct) + "%)";
                PostMessageW(g_setup.hwnd, WM_APP + 10, 0, (LPARAM)new std::string(msg));
            }
        }
    };
        setStatusDetail(std::string(lang("setup_checking_java")) + " 21...");
    std::wstring javaPath;
    if (!ravex::game::ensureJava(21, javaPath, &err, [](const std::string& s){ PostMessageW(g_setup.hwnd, WM_APP + 10, 0, (LPARAM)new std::string(s)); }, nullptr)) {
        setStatusDetail(std::string(lang("setup_checking_java")) + " fail: " + err);
        EnableWindow(g_setup.hInstall, TRUE);
        g_setup.busy = false;
        return;
    }
    Sleep(400);
    setStatusDetail(lang("setup_verifying_java"));
    Sleep(500);
    if (!fileExists(javaPath)) { setStatusDetail(std::string(lang("setup_verifying_java")) + " fail"); EnableWindow(g_setup.hInstall, TRUE); g_setup.busy=false; return; }
    appendDetail("Java 21 OK: " + toUtf8(javaPath));
    Sleep(300);
    setStatusDetail(std::string(lang("setup_checking_mc")) + " " + version + "...");
    std::string assetId;
    bool mcOk = false;
    for (int attempt = 1; attempt <= 3; ++attempt) {
        err.clear();
        if (attempt > 1) {
            appendDetail("Retrying Minecraft download, attempt " + std::to_string(attempt) + "/3 ...");
            setStatusDetail(std::string(lang("setup_checking_mc")) + " " + version + " (retry " + std::to_string(attempt) + ")...");
            Sleep(1200);
        }
        if (!ravex::game::ensureMinecraft(version, &err, postProgress, nullptr, &assetId)) {
            setStatusDetail(std::string(lang("setup_checking_mc")) + " fail: " + err);
            appendDetail("ensureMinecraft attempt " + std::to_string(attempt) + " failed: " + err);
            if (attempt == 3) { EnableWindow(g_setup.hInstall, TRUE); g_setup.busy = false; return; }
            continue;
        }
        Sleep(400);
        setStatusDetail(lang("setup_verifying_mc"));
        Sleep(600);
        std::string verr;
        if (!ravex::game::quickIntegrityCheck(version, assetId, &verr)) {
            appendDetail("Integrity attempt " + std::to_string(attempt) + " failed: " + verr);
            if (attempt == 3) {
                setStatusDetail(std::string(lang("setup_verifying_mc")) + " fail: " + verr);
                EnableWindow(g_setup.hInstall, TRUE);
                g_setup.busy = false;
                return;
            }
            continue;
        }
        appendDetail("Integrity OK (attempt " + std::to_string(attempt) + ")");
        Sleep(300);
        mcOk = true;
        break;
    }
    if (!mcOk) { EnableWindow(g_setup.hInstall, TRUE); g_setup.busy = false; return; }
    setStatusDetail(lang("setup_preparing"));
    Sleep(400);
    std::wstring instDir = joinPath(joinPath(kickxDir(), L"instances"), L"Default");
    createDirs(instDir);
    InstanceCfg cfg;
    cfg.name = "Default";
    cfg.mcVersion = version;
    cfg.loader = "vanilla";
    cfg.assetIndexId = assetId;
    saveInstance(instDir, cfg);
    if (g_setup.desktopChecked) {
        PWSTR pDesk = nullptr; if (SHGetKnownFolderPath(FOLDERID_Desktop, 0, nullptr, &pDesk) == S_OK) {
            std::wstring link = std::wstring(pDesk) + L"\\KickX.lnk";
            createShortcut(dstLauncher, link, fromUtf8(lang("setup_title")).c_str());
            appendDetail(lang("setup_shortcut_desktop_created") ? lang("setup_shortcut_desktop_created") : "Desktop shortcut created");
            CoTaskMemFree(pDesk);
        }
    }
    if (g_setup.startChecked) {
        PWSTR pStart = nullptr; if (SHGetKnownFolderPath(FOLDERID_StartMenu, 0, nullptr, &pStart) == S_OK) {
            std::wstring dir = std::wstring(pStart) + L"\\KickX";
            createDirs(dir);
            std::wstring link = dir + L"\\KickX.lnk";
            createShortcut(dstLauncher, link, fromUtf8(lang("setup_title")).c_str());
            appendDetail(lang("setup_shortcut_start_created") ? lang("setup_shortcut_start_created") : "Start Menu shortcut created");
            CoTaskMemFree(pStart);
        }
    }
    std::wstring marker = joinPath(kickxDir(), L".setup_done");
    HANDLE h = CreateFileW(marker.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_HIDDEN, nullptr);
    if (h != INVALID_HANDLE_VALUE) CloseHandle(h);
    createUninstallRegistry(installDir);
    setStatusDetail(lang("setup_verifying_install"));
    Sleep(600);
    bool ok = fileExists(dstLauncher) && fileExists(dstSetup) && fileExists(joinPath(kickxDir(), L".setup_done"));
    appendDetail(ok ? (lang("setup_verifying_install") ? std::string(lang("setup_verifying_install")) + " OK" : "Verification: all components installed") : "Verification: missing files");
    Sleep(400);
    setStatusDetail(lang("setup_done"));
    g_setup.busy = false;
    EnableWindow(g_setup.hInstall, TRUE);
    PostMessageW(g_setup.hwnd, WM_APP + 11, 0, (LPARAM)new std::wstring(installDir));
}
LRESULT CALLBACK SetupProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_CREATE: {
            g_setup.hwnd = hwnd;
            g_setup.theme = getTheme("dark");
            g_setup.theme.bg = RGB(18,18,18);
            g_setup.theme.panel = RGB(30,30,30);
            g_setup.theme.text = RGB(240,240,240);
            g_setup.theme.accent = RGB(0,120,212);
            g_setup.theme.buttonBg = RGB(45,45,47);
            g_setup.theme.isDark = true;
            g_setup.bgBrush = CreateSolidBrush(g_setup.theme.bg);
            g_setup.font = makeSetupFont();
            g_setup.titleFont = makeTitleFont();
            HINSTANCE inst = (HINSTANCE)GetWindowLongPtrW(hwnd, GWLP_HINSTANCE);
                    g_setup.hTitle = CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_title")).c_str(), WS_CHILD | WS_VISIBLE | SS_CENTER, 0, 10, 560, 26, hwnd, nullptr, inst, nullptr);
            g_setup.hLangLabel = CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_lang")).c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 50, 500, 22, hwnd, nullptr, inst, nullptr);
            g_setup.hLangCombo = CreateWindowExW(0, L"COMBOBOX", nullptr, WS_CHILD | WS_VISIBLE | CBS_DROPDOWNLIST | WS_VSCROLL, 20, 74, 300, 200, hwnd, (HMENU)IDC_LANG, inst, nullptr);
            for (int i = 0; i < langCount(); ++i) {
                const char* code = langCodeByIndex(i);
                const char* name = langDisplayName(code);
                std::wstring w = fromUtf8(name);
                SendMessageW(g_setup.hLangCombo, CB_ADDSTRING, 0, (LPARAM)w.c_str());
            }
            SendMessageW(g_setup.hLangCombo, CB_SETCURSEL, 0, 0);
            {
                LauncherConfig lc = loadLauncherConfig();
                for (int i = 0; i < langCount(); ++i) if (std::string(langCodeByIndex(i)) == lc.language) { SendMessageW(g_setup.hLangCombo, CB_SETCURSEL, i, 0); setCurrentLanguage(langCodeByIndex(i)); break; }
            }
            g_setup.hWelcome = CreateWindowExW(0, L"STATIC", fromUtf8(std::string(lang("setup_welcome")) + "\r\n" + lang("setup_welcome2")).c_str(), WS_CHILD | WS_VISIBLE | SS_CENTER, 20, 55, 520, 40, hwnd, nullptr, inst, nullptr);
            CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_dest")).c_str(), WS_CHILD | WS_VISIBLE, 20, 55, 520, 22, hwnd, (HMENU)3001, inst, nullptr);
            g_setup.hPath = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 20, 80, 380, 24, hwnd, (HMENU)IDC_PATH, inst, nullptr);
            g_setup.hBrowse = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_browse")).c_str(), WS_CHILD | WS_VISIBLE, 410, 80, 90, 24, hwnd, (HMENU)IDC_BROWSE, inst, nullptr);
            CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_start")).c_str(), WS_CHILD | WS_VISIBLE, 20, 55, 520, 22, hwnd, (HMENU)3002, inst, nullptr);
            g_setup.hStartMenu = CreateWindowExW(0, L"EDIT", L"KickX", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 20, 80, 520, 24, hwnd, (HMENU)IDC_STARTMENU, inst, nullptr);
            g_setup.hShortcutDesktop = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_shortcut_desktop")).c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 20, 65, 260, 28, hwnd, (HMENU)IDC_SHORTCUT_DESKTOP, inst, nullptr);
            g_setup.hShortcutStart = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_shortcut_start")).c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 20, 100, 280, 28, hwnd, (HMENU)IDC_SHORTCUT_START, inst, nullptr);
            g_setup.hSummary = CreateWindowExW(0, L"STATIC", L"", WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 36, 500, 60, hwnd, nullptr, inst, nullptr);
            SendMessageW(g_setup.hShortcutDesktop, BM_SETCHECK, BST_CHECKED, 0);
            SendMessageW(g_setup.hShortcutStart, BM_SETCHECK, BST_CHECKED, 0);
            PWSTR pLocal = nullptr; if (SHGetKnownFolderPath(FOLDERID_LocalAppData, 0, nullptr, &pLocal) == S_OK) { std::wstring def = std::wstring(pLocal) + L"\\KickX"; SetWindowTextW(g_setup.hPath, def.c_str()); CoTaskMemFree(pLocal); } else SetWindowTextW(g_setup.hPath, L"C:\\KickX");
            g_setup.hStatus = CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_ready_install")).c_str(), WS_CHILD | WS_VISIBLE | SS_CENTER, 0, 98, 540, 16, hwnd, nullptr, inst, nullptr);
            g_setup.hProgress = CreateWindowExW(0, PROGRESS_CLASSW, nullptr, WS_CHILD, 20, 120, 500, 16, hwnd, (HMENU)IDC_PROGRESS, inst, nullptr);
            g_setup.hDetailsBtn = CreateWindowExW(0, L"BUTTON", fromUtf8(std::string(lang("setup_details")) + "  \xE2\x96\xBC").c_str(), WS_CHILD | WS_VISIBLE, 20, 150, 110, 22, hwnd, (HMENU)IDC_DETAILS, inst, nullptr);
            g_setup.hDetailsEdit = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"", WS_CHILD | WS_VSCROLL | ES_MULTILINE | ES_READONLY | ES_AUTOVSCROLL, 20, 176, 540, 120, hwnd, nullptr, inst, nullptr);
            g_setup.hInstall = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_install")).c_str(), WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON, 160, 180, 100, 32, hwnd, (HMENU)IDC_INSTALL, inst, nullptr);
            g_setup.hNext = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_next")).c_str(), WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON, 380, 200, 80, 28, hwnd, (HMENU)IDC_NEXT, inst, nullptr);
            g_setup.hBack = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_back")).c_str(), WS_CHILD | WS_VISIBLE, 280, 200, 80, 28, hwnd, (HMENU)IDC_BACK, inst, nullptr);
            SendMessageW(g_setup.hTitle, WM_SETFONT, (WPARAM)g_setup.titleFont, TRUE);
            SendMessageW(g_setup.hLangLabel, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hLangCombo, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hStatus, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hDetailsBtn, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hDetailsEdit, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hInstall, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hNext, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hBack, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            updateLangTexts();
            SendMessageW(g_setup.hPath, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hBrowse, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hShortcutDesktop, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hShortcutStart, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hWelcome, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hSummary, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hStartMenu, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(GetDlgItem(hwnd, 3001), WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(GetDlgItem(hwnd, 3002), WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SetWindowTheme(g_setup.hPath, L"", L"");
            SetWindowTheme(g_setup.hStartMenu, L"", L"");
            SetWindowTheme(g_setup.hDetailsEdit, L"", L"");
            showPageImmediate(0);
            ravex::ui::glowCreate(hwnd, &g_setup.glow);
            g_setup.glow.anywhere = true;
            g_setup.glow.parent = hwnd;
            if (g_setup.glow.bmp) {
                BITMAP bm{}; GetObjectW(g_setup.glow.bmp, sizeof(bm), &bm);
                HDC hdcScreen = GetDC(nullptr);
                BITMAPINFO bi{}; bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER); bi.bmiHeader.biWidth = 160; bi.bmiHeader.biHeight = -160; bi.bmiHeader.biPlanes=1; bi.bmiHeader.biBitCount=32; bi.bmiHeader.biCompression=BI_RGB;
                void* pv = nullptr;
                HBITMAP whiteBmp = CreateDIBSection(hdcScreen, &bi, DIB_RGB_COLORS, &pv, nullptr, 0);
                ReleaseDC(nullptr, hdcScreen);
                if (pv) {
                    BYTE* px=(BYTE*)pv;
                    for(int y=0;y<160;++y) for(int x=0;x<160;++x){
                        float dx=x-80, dy=y-80; float dist=sqrtf(dx*dx+dy*dy); float t=dist/75.0f; if(t>1) t=1; float s=1-t; float a=s*s*s*60;
                        BYTE al=(BYTE)(a>255?255:a); float f=al/255.0f; int o=(y*160+x)*4; px[o+0]= (BYTE)(140*f); px[o+1]=(BYTE)(180*f); px[o+2]=(BYTE)(255*f); px[o+3]=al;
                    }
                    DeleteObject(g_setup.glow.bmp);
                    g_setup.glow.bmp = whiteBmp;
                }
            }
            {
                std::vector<HWND> btns = {g_setup.hNext, g_setup.hBack, g_setup.hInstall, g_setup.hBrowse, g_setup.hDetailsBtn, g_setup.hShortcutDesktop, g_setup.hShortcutStart};
                ravex::ui::glowSetButtons(&g_setup.glow, btns);
            }
            SetTimer(hwnd, 52, 16, nullptr);
            return 0;
        }
        case WM_COMMAND: {
            int id = LOWORD(wParam);
            if (id == IDC_BROWSE) {
                BROWSEINFOW bi{}; bi.hwndOwner = hwnd; bi.lpszTitle = fromUtf8(lang("setup_select_folder")).c_str(); bi.ulFlags = BIF_RETURNONLYFSDIRS | BIF_NEWDIALOGSTYLE;
                LPITEMIDLIST pidl = SHBrowseForFolderW(&bi);
                if (pidl) { wchar_t path[MAX_PATH]; if (SHGetPathFromIDListW(pidl, path)) SetWindowTextW(g_setup.hPath, path); CoTaskMemFree(pidl); }
                return 0;
            }
            if (id == IDC_SHORTCUT_DESKTOP) { g_setup.desktopChecked = !g_setup.desktopChecked; InvalidateRect(g_setup.hShortcutDesktop, nullptr, TRUE); return 0; }
            if (id == IDC_SHORTCUT_START) { g_setup.startChecked = !g_setup.startChecked; InvalidateRect(g_setup.hShortcutStart, nullptr, TRUE); return 0; }
            if (id == IDC_DETAILS) {
                g_setup.detailsVisible = !g_setup.detailsVisible;
                ShowWindow(g_setup.hDetailsEdit, g_setup.detailsVisible ? SW_SHOW : SW_HIDE);
                RECT rc; GetClientRect(hwnd, &rc);
                SendMessageW(hwnd, WM_SIZE, 0, MAKELPARAM(rc.right, rc.bottom));
                return 0;
            }
            if (id == IDC_LANG && HIWORD(wParam) == CBN_SELCHANGE) {
                int sel = (int)SendMessageW(g_setup.hLangCombo, CB_GETCURSEL, 0, 0);
                if (sel >= 0) {
                    const char* code = langCodeByIndex(sel);
                    setCurrentLanguage(code);
                    LauncherConfig lc = loadLauncherConfig(); lc.language = code; saveLauncherConfig(lc);
                    updateLangTexts();
                    InvalidateRect(hwnd, nullptr, TRUE);
                }
                return 0;
            }
            if (id == IDC_NEXT) {
                if (g_setup.page < 5) showPage(g_setup.page + 1);
                return 0;
            }
            if (id == IDC_BACK) {
                if (g_setup.page > 0) showPage(g_setup.page - 1);
                return 0;
            }
            if (id == IDC_INSTALL && !g_setup.busy) {
                showPage(6);
                std::thread(workerSetup).detach();
                return 0;
            }
            return 0;
        }
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds = (DRAWITEMSTRUCT*)lParam;
            if (ds->CtlID == IDC_SHORTCUT_DESKTOP || ds->CtlID == IDC_SHORTCUT_START) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool chk = (ds->CtlID == IDC_SHORTCUT_DESKTOP) ? g_setup.desktopChecked : g_setup.startChecked;
                FillRect(dc, &rc, g_setup.bgBrush);
                int box = 18; int by = rc.top + (rc.bottom - rc.top - box)/2; int bx = rc.left + 4;
                Gdiplus::Graphics g(dc);
                g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                Gdiplus::Color fill = chk ? Gdiplus::Color(255, 0,120,212) : Gdiplus::Color(255,45,45,47);
                Gdiplus::Color border = chk ? Gdiplus::Color(255, 0,120,212) : Gdiplus::Color(255,110,110,115);
                Gdiplus::SolidBrush br(fill);
                Gdiplus::Pen pen(border, 1.5f);
                Gdiplus::GraphicsPath path;
                path.AddArc(bx, by, 3, 3, 180, 90); path.AddArc(bx+box-3, by, 3, 3, 270, 90); path.AddArc(bx+box-3, by+box-3, 3, 3, 0, 90); path.AddArc(bx, by+box-3, 3, 3, 90, 90); path.CloseFigure();
                g.FillPath(&br, &path); g.DrawPath(&pen, &path);
                if (chk) {
                    Gdiplus::Pen cpen(Gdiplus::Color(255,255,255,255), 1.8f);
                    cpen.SetLineCap(Gdiplus::LineCapRound, Gdiplus::LineCapRound, Gdiplus::DashCapFlat);
                    g.DrawLine(&cpen, bx+5, by+9, bx+8, by+12); g.DrawLine(&cpen, bx+8, by+12, bx+13, by+6);
                }
                Gdiplus::SolidBrush tbrush(Gdiplus::Color(255,240,240,240));
                Gdiplus::Font f(dc, g_setup.font);
                RECT tr = {bx+box+10, rc.top, rc.right, rc.bottom};
                Gdiplus::RectF rf((Gdiplus::REAL)tr.left, (Gdiplus::REAL)tr.top, (Gdiplus::REAL)(tr.right-tr.left), (Gdiplus::REAL)(tr.bottom-tr.top));
                Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentNear); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                wchar_t txt[128]; GetWindowTextW(ds->hwndItem, txt, 128);
                g.DrawString(txt, -1, &f, rf, &fmt, &tbrush);
                return 1;
            }
            return 0;
        }
        case WM_CTLCOLORSTATIC: {
            HDC dc = (HDC)wParam;
            SetTextColor(dc, g_setup.theme.text);
            SetBkColor(dc, g_setup.theme.bg);
            SetBkMode(dc, TRANSPARENT);
            return (LRESULT)g_setup.bgBrush;
        }
        case WM_CTLCOLORBTN: {
            HDC dc = (HDC)wParam;
            SetTextColor(dc, g_setup.theme.text);
            SetBkColor(dc, g_setup.theme.bg);
            SetBkMode(dc, TRANSPARENT);
            return (LRESULT)g_setup.bgBrush;
        }
        case WM_CTLCOLOREDIT:
        case WM_CTLCOLORLISTBOX: {
            HDC dc = (HDC)wParam;
            SetTextColor(dc, g_setup.theme.text);
            SetBkColor(dc, g_setup.theme.panel);
            SetBkMode(dc, OPAQUE);
            return (LRESULT)g_setup.bgBrush;
        }
        case WM_TIMER: {
            if (wParam == 50 && g_setup.page == 6) {
                g_setup.spinnerPhase += 0.22f;
                if (g_setup.spinnerPhase > 6.283f) g_setup.spinnerPhase -= 6.283f;
                RECT rc; GetClientRect(hwnd, &rc);
                RECT sr = { (rc.right-60)/2, 120, (rc.right+60)/2, 180 };
                InvalidateRect(hwnd, &sr, FALSE);
                return 0;
            }
            if (wParam == 51) {
                g_setup.slideOffset += 6;
                if (g_setup.slideOffset >= 0) { g_setup.slideOffset = 0; KillTimer(hwnd, 51); }
                RECT rc; GetClientRect(hwnd, &rc);
                int w = rc.right - rc.left;
                int h = rc.bottom - rc.top;
                int so = g_setup.slideOffset;
                HDWP dwp = BeginDeferWindowPos(10);
                if (dwp) {
                    auto placeCard = [&](HWND hw, int x, int y, int cw, int ch){ DeferWindowPos(dwp, hw, nullptr, x, y+so, cw, ch, SWP_NOZORDER | SWP_NOOWNERZORDER | SWP_NOCOPYBITS); };
                    placeCard(g_setup.hLangCombo, 20, 74, 300, 200);
                    placeCard(g_setup.hLangLabel, 20, 50, w-40, 22);
                    placeCard(g_setup.hPath, 20, 80, w - 140, 24);
                    placeCard(GetDlgItem(hwnd, 3001), 20, 55, w-40, 22);
                    placeCard(g_setup.hStartMenu, 20, 80, w-40, 24);
                    placeCard(GetDlgItem(hwnd, 3002), 20, 55, w-40, 22);
                    placeCard(g_setup.hShortcutDesktop, 20, 65, 260, 28);
                    placeCard(g_setup.hShortcutStart, 20, 100, 280, 28);
                    placeCard(g_setup.hStatus, 0, 98, w, 16);
                    placeCard(g_setup.hDetailsBtn, 20, 120, 90, 22);
                    placeCard(g_setup.hSummary, 20, 55, w-40, 60);
                    placeCard(g_setup.hWelcome, 20, 55, w-40, 40);
                    EndDeferWindowPos(dwp);
                }
                RECT card = {0, 35, w, h-55};
                InvalidateRect(hwnd, &card, FALSE);
                return 0;
            }
            if (wParam == 52) { ravex::ui::glowUpdateAnywhere(hwnd, &g_setup.glow); return 0; }
            return 0;
        }
        case WM_PAINT: {
            if (g_setup.page == 6) {
                PAINTSTRUCT ps; HDC hdc = BeginPaint(hwnd, &ps);
                FillRect(hdc, &ps.rcPaint, g_setup.bgBrush);
                RECT rc; GetClientRect(hwnd, &rc);
                int cx = rc.right/2; int cy = 150;
                drawSpinner(hdc, cx, cy, g_setup.spinnerPhase);
                EndPaint(hwnd, &ps);
                return 0;
            }
            return DefWindowProcW(hwnd, msg, wParam, lParam);
        }
        case WM_MOUSEMOVE: { ravex::ui::glowUpdateAnywhere(hwnd, &g_setup.glow); return 0; }
        case WM_APP + 10: {
            std::string* p = (std::string*)lParam;
            setStatus(*p); appendDetail(*p); delete p; return 0;
        }
        case WM_APP + 11: {
            std::wstring* p = (std::wstring*)lParam;
            KillTimer(hwnd, 50);
            g_setup.busy = false;
            { std::wstring msg = fromUtf8(lang("setup_installed_msg")); size_t pos = msg.find(L"{path}"); if(pos != std::wstring::npos) msg.replace(pos, 6, *p);
            MessageBoxW(hwnd, msg.c_str(), fromUtf8(lang("setup_title")).c_str(), MB_OK | MB_ICONINFORMATION); }
            delete p;
            DestroyWindow(hwnd);
            return 0;
        }
        case WM_GETMINMAXINFO: {
            MINMAXINFO* mi = (MINMAXINFO*)lParam;
            mi->ptMinTrackSize.x = 520;
            mi->ptMinTrackSize.y = 400;
            return 0;
        }
        case WM_SIZE: {
            RECT rc; GetClientRect(hwnd, &rc);
            int w = rc.right - rc.left;
            int h = rc.bottom - rc.top;
            int so = g_setup.slideOffset;
            HDWP dwp = BeginDeferWindowPos(14);
            if (dwp) {
                auto placeFixed = [&](HWND hw, int x, int y, int cw, int ch){ DeferWindowPos(dwp, hw, nullptr, x, y, cw, ch, SWP_NOZORDER); };
                auto placeCard = [&](HWND hw, int x, int y, int cw, int ch){ DeferWindowPos(dwp, hw, nullptr, x, y+so, cw, ch, SWP_NOZORDER); };
                placeFixed(g_setup.hTitle, 0, 10, w, 26);
                placeFixed(g_setup.hNext, w - 100, h - 50, 80, 28);
                placeFixed(g_setup.hBack, w - 190, h - 50, 80, 28);
                placeFixed(g_setup.hInstall, w/2 - 50, h - 50, 100, 32);
                placeCard(g_setup.hLangCombo, 20, 74, 300, 200);
                placeCard(g_setup.hLangLabel, 20, 50, w-40, 22);
                placeCard(g_setup.hPath, 20, 80, w - 140, 24);
                placeCard(g_setup.hBrowse, w - 110, 80, 90, 24);
                placeCard(g_setup.hStartMenu, 20, 80, w-40, 24);
                placeCard(g_setup.hShortcutDesktop, 20, 65, 260, 28);
                placeCard(g_setup.hShortcutStart, 20, 100, 280, 28);
                placeCard(g_setup.hStatus, 0, 98, w, 16);
                placeCard(g_setup.hDetailsBtn, 20, 120, 90, 22);
                int editTop = g_setup.detailsVisible ? 150 : 176;
                int editH = g_setup.detailsVisible ? (h - editTop - 60) : 0;
                if (editH < 0) editH = 0;
                placeCard(g_setup.hDetailsEdit, 20, editTop, w-40, editH);
                placeCard(g_setup.hSummary, 20, 55, w-40, 60);
                placeCard(g_setup.hWelcome, 20, 55, w-40, 40);
                placeCard(GetDlgItem(hwnd, 3001), 20, 55, w-40, 22);
                placeCard(GetDlgItem(hwnd, 3002), 20, 55, w-40, 22);
                EndDeferWindowPos(dwp);
            }
            return 0;
        }
        case WM_ERASEBKGND: {
            HDC dc = (HDC)wParam; RECT rc; GetClientRect(hwnd, &rc); FillRect(dc, &rc, g_setup.bgBrush); return 1;
        }
        case WM_DESTROY: {
            KillTimer(hwnd, 50); KillTimer(hwnd, 51); KillTimer(hwnd, 52);
            ravex::ui::glowDestroy(&g_setup.glow);
            if (g_setup.font) DeleteObject(g_setup.font);
            if (g_setup.titleFont) DeleteObject(g_setup.titleFont);
            if (g_setup.bgBrush) DeleteObject(g_setup.bgBrush);
            PostQuitMessage(0);
            return 0;
        }
        default: return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}
}
bool isKickXInstalled(std::wstring* outPath) {
    PWSTR pLocal=nullptr;
    if (SHGetKnownFolderPath(FOLDERID_LocalAppData,0,nullptr,&pLocal)==S_OK) {
        std::wstring def = std::wstring(pLocal)+L"\\KickX\\kickx_launcher.exe";
        if (fileExists(def)) { if(outPath) *outPath = std::wstring(pLocal)+L"\\KickX"; CoTaskMemFree(pLocal); return true; }
        CoTaskMemFree(pLocal);
    }
    if (fileExists(joinPath(kickxDir(), L".setup_done"))) {
        if(outPath) *outPath = kickxDir();
        return true;
    }
    HKEY hk; if (RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\KickX",0,KEY_READ,&hk)==ERROR_SUCCESS) {
        wchar_t buf[MAX_PATH]; DWORD sz=sizeof(buf);
        if (RegQueryValueExW(hk,L"InstallLocation",nullptr,nullptr,(BYTE*)buf,&sz)==ERROR_SUCCESS) { if(outPath) *outPath=buf; RegCloseKey(hk); return true; }
        RegCloseKey(hk); return true;
    }
    return false;
}
void createUninstallRegistry(const std::wstring& installDir) {
    HKEY hk; DWORD disp;
    if (RegCreateKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\KickX",0,nullptr,0,KEY_WRITE,nullptr,&hk,&disp)==ERROR_SUCCESS) {
        RegCloseKey(hk);
        if (RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\KickX",0,KEY_WRITE,&hk)==ERROR_SUCCESS) {
            std::wstring uninstall = L"\"" + joinPath(installDir, L"KickXSetup.exe") + L"\" /uninstall";
            std::wstring display = L"KickX Launcher";
            std::wstring publisher = L"StormDevzz";
            DWORD est = 250000;
            RegSetValueExW(hk,L"DisplayName",0,REG_SZ,(BYTE*)display.c_str(), (DWORD)((display.size()+1)*2));
            RegSetValueExW(hk,L"UninstallString",0,REG_SZ,(BYTE*)uninstall.c_str(), (DWORD)((uninstall.size()+1)*2));
            RegSetValueExW(hk,L"InstallLocation",0,REG_SZ,(BYTE*)installDir.c_str(), (DWORD)((installDir.size()+1)*2));
            RegSetValueExW(hk,L"Publisher",0,REG_SZ,(BYTE*)publisher.c_str(), (DWORD)((publisher.size()+1)*2));
            RegSetValueExW(hk,L"DisplayVersion",0,REG_SZ,(BYTE*)L"1.1", 8);
            RegSetValueExW(hk,L"EstimatedSize",0,REG_DWORD,(BYTE*)&est, sizeof(est));
            std::wstring displayIcon = joinPath(installDir, L"kickx_launcher.exe") + L",0";
            RegSetValueExW(hk,L"DisplayIcon",0,REG_SZ,(BYTE*)displayIcon.c_str(), (DWORD)((displayIcon.size()+1)*2));
            {
                std::wstring urlAbout = L"https://github.com/StormDevzz";
                std::wstring helpLink = L"https://github.com/StormDevzz";
                std::wstring src = L"https://github.com/StormDevzz";
                RegSetValueExW(hk,L"URLInfoAbout",0,REG_SZ,(BYTE*)urlAbout.c_str(), (DWORD)((urlAbout.size()+1)*2));
                RegSetValueExW(hk,L"HelpLink",0,REG_SZ,(BYTE*)helpLink.c_str(), (DWORD)((helpLink.size()+1)*2));
                RegSetValueExW(hk,L"InstallSource",0,REG_SZ,(BYTE*)src.c_str(), (DWORD)((src.size()+1)*2));
            }
            DWORD noMod=1;
            RegSetValueExW(hk,L"NoModify",0,REG_DWORD,(BYTE*)&noMod, sizeof(DWORD));
            RegCloseKey(hk);
        }
    }
}
void removeUninstallRegistry() {
    RegDeleteKeyW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\KickX");
}
bool performUninstall(const std::wstring& installDir) {
    std::wstring dir = installDir;
    if (dir.empty()) { PWSTR p=nullptr; if(SHGetKnownFolderPath(FOLDERID_LocalAppData,0,nullptr,&p)==S_OK){ dir=std::wstring(p)+L"\\KickX"; CoTaskMemFree(p);} else dir=kickxDir(); }
    PWSTR pDesk=nullptr; if(SHGetKnownFolderPath(FOLDERID_Desktop,0,nullptr,&pDesk)==S_OK){ DeleteFileW((std::wstring(pDesk)+L"\\KickX.lnk").c_str()); CoTaskMemFree(pDesk); }
    PWSTR pStart=nullptr; if(SHGetKnownFolderPath(FOLDERID_StartMenu,0,nullptr,&pStart)==S_OK){ DeleteFileW((std::wstring(pStart)+L"\\KickX\\KickX.lnk").c_str()); RemoveDirectoryW((std::wstring(pStart)+L"\\KickX").c_str()); CoTaskMemFree(pStart); }
    DeleteFileW(joinPath(dir, L"kickx_launcher.exe").c_str());
    DeleteFileW(joinPath(dir, L"KickXSetup.exe").c_str());
    SHFILEOPSTRUCTW op{}; op.wFunc=FO_DELETE; std::wstring pFrom=dir+L'\0'; pFrom.push_back(L'\0'); op.pFrom=pFrom.c_str(); op.fFlags=FOF_NOCONFIRMATION|FOF_NOERRORUI|FOF_SILENT; SHFileOperationW(&op);
    DeleteFileW(joinPath(kickxDir(), L".setup_done").c_str());
    removeUninstallRegistry();
    return true;
}
bool runUninstallWindow(HINSTANCE inst) {
    { LauncherConfig lc = loadLauncherConfig(); setCurrentLanguage(lc.language.c_str()); }
    std::wstring path; isKickXInstalled(&path);
    std::wstring uninstallMsg = fromUtf8(lang("setup_uninstall_confirm")); size_t uPos = uninstallMsg.find(L"{path}"); if(uPos != std::wstring::npos) uninstallMsg.replace(uPos, 6, path.empty()?L"unknown":path);
    int r = MessageBoxW(nullptr, uninstallMsg.c_str(), fromUtf8(lang("setup_title")).c_str(), MB_YESNO | MB_ICONWARNING);
    if (r != IDYES) return true;
    performUninstall(path);
    MessageBoxW(nullptr, fromUtf8(lang("setup_uninstalled_msg")).c_str(), fromUtf8(lang("setup_title")).c_str(), MB_OK | MB_ICONINFORMATION);
    return true;
}
bool runSetupWindow(HINSTANCE inst) {
    Gdiplus::GdiplusStartupInput si{}; ULONG_PTR tok = 0; Gdiplus::GdiplusStartup(&tok, &si, nullptr);
    { LauncherConfig lc = loadLauncherConfig(); setCurrentLanguage(lc.language.c_str()); }
    std::wstring existing;
    if (isKickXInstalled(&existing)) {
        std::wstring alreadyMsg = fromUtf8(lang("setup_already_found")); size_t aPos = alreadyMsg.find(L"{path}"); if(aPos != std::wstring::npos) alreadyMsg.replace(aPos, 6, existing);
        int r = MessageBoxW(nullptr, alreadyMsg.c_str(), fromUtf8(lang("setup_title")).c_str(), MB_YESNOCANCEL | MB_ICONWARNING);
        if (r == IDNO) { Gdiplus::GdiplusShutdown(tok); runUninstallWindow(inst); return true; }
        if (r == IDCANCEL) { Gdiplus::GdiplusShutdown(tok); return true; }
    }
    WNDCLASSEXW wc{}; wc.cbSize = sizeof(wc); wc.lpfnWndProc = SetupProc; wc.hInstance = inst;
    wc.hIcon = LoadIconW(inst, MAKEINTRESOURCEW(101)); if (!wc.hIcon) wc.hIcon = LoadIconW(nullptr, MAKEINTRESOURCEW(32512));
    wc.hCursor = LoadCursorW(nullptr, MAKEINTRESOURCEW(32512)); wc.lpszClassName = L"KickXSetup"; wc.hbrBackground = (HBRUSH)(COLOR_WINDOW+1);
    RegisterClassExW(&wc);
    HWND hwnd = CreateWindowExW(0, L"KickXSetup", fromUtf8(lang("setup_title")).c_str(), WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN | WS_CLIPSIBLINGS, CW_USEDEFAULT, CW_USEDEFAULT, 600, 400, nullptr, nullptr, inst, nullptr);
    if (!hwnd) { Gdiplus::GdiplusShutdown(tok); return false; }
    ShowWindow(hwnd, SW_SHOW); UpdateWindow(hwnd);
    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0)) { TranslateMessage(&msg); DispatchMessageW(&msg); }
    Gdiplus::GdiplusShutdown(tok);
    return true;
}
}
