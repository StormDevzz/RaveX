#include "setup/include/setup_window.hpp"
#include "core/include/config.hpp"
#include "core/include/paths.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "core/include/lang.hpp"
#include "build_info.h"
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
#include <algorithm>
#include <vector>
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
constexpr int IDC_CANCEL_SETUP = 2015;
constexpr int IDC_DISK_INFO = 2016;
constexpr int IDC_DISK_BAR = 2017;
constexpr int IDC_AUTOSTART = 2018;
constexpr int IDC_AUTOLAUNCH = 2019;
constexpr int IDC_THEME_SETUP = 2020;
constexpr int IDC_COLOR_BG = 2030;      // palette swatches .. IDC_COLOR_GLOW = 2035
constexpr UINT_PTR TIMER_PAGE_ANIM = 60;
constexpr UINT_PTR TIMER_CHECK_ANIM = 61;
constexpr int PAGE_ANIM_MS = 260;   // page transition duration
constexpr int PAGE_SLIDE = 60;      // slide distance in pixels (down = out, up = in)
constexpr int CHECK_ANIM_MS = 150;  // checkbox tick appear/disappear
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
    HWND hThemeCombo = nullptr;
    HWND hThemeLabel = nullptr;
    HWND hPaletteLabel = nullptr;
    HWND hColorBtns[6] = {};
    // custom palette cache - the SAME launcher.json fields the launcher uses.
    uint32_t palColors[6] = {0x1C1C1E, 0x282828, 0xE1E1E1, 0x5A8CFF, 0x5A8CFF, 0xC8E1FF};
    bool themeIsCustom = false;
    HWND hDiskIcon = nullptr;
    HICON hDiskIco = nullptr;
    std::vector<HBITMAP> flagBmps;
    // smooth appear/disappear of the checkbox ticks (order: desktop, start,
    // autostart, autolaunch).
    float chkAnim[4] = {1.0f, 1.0f, 0.0f, 1.0f};
    bool chkTarget[4] = {true, true, false, true};
    HWND hWinIcon = nullptr;
    HWND hCancelSetup = nullptr;
    HWND hDiskInfo = nullptr;
    HWND hDiskBar = nullptr;
    HWND hAutoStart = nullptr;
    HWND hAutoLaunch = nullptr;
    bool autoStartChecked = false;
    bool autoLaunchChecked = true;
    bool cancelRequested = false;
    bool winIconHovered = false;
    float winIconScale = 1.0f;
    float winIconTarget = 1.0f;
    Gdiplus::Bitmap* winIconBmp = nullptr;
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
    // page transition animation: outgoing content slides down & fades out,
    // incoming content slides down from the top & fades in.
    bool animActive = false;
    float animT = 0.0f;
    std::vector<HWND> animOut, animIn;
    std::vector<POINT> animOutP0, animInP0;
    ITaskbarList3* taskbar = nullptr;
    bool taskbarReady = false;
};
SetupData g_setup;
HFONT makeSetupFont() {
    // grayscale antialiasing instead of ClearType: ClearType is tuned for light
    // backgrounds and produces pixelated-looking color fringes on our dark one.
    HFONT f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, ANTIALIASED_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    if (!f) f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, ANTIALIASED_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    return f;
}
HFONT makeTitleFont() {
    HFONT f = CreateFontW(-19, 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, ANTIALIASED_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    if (!f) f = CreateFontW(-19, 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, ANTIALIASED_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    return f;
}
// --- language flags (same visual style as the Settings dialog) -------------
HBITMAP loadFlagBmp(const std::wstring& path, int w = 24, int h = 16) {
    Gdiplus::Bitmap* bmp = Gdiplus::Bitmap::FromFile(path.c_str());
    if (!bmp || bmp->GetLastStatus() != Gdiplus::Ok) { delete bmp; return nullptr; }
    Gdiplus::Bitmap* scaled = new Gdiplus::Bitmap(w, h, PixelFormat32bppARGB);
    Gdiplus::Graphics g(scaled);
    g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
    g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
    g.SetPixelOffsetMode(Gdiplus::PixelOffsetModeHighQuality);
    g.SetCompositingQuality(Gdiplus::CompositingQualityHighQuality);
    g.Clear(Gdiplus::Color(0,0,0,0));
    g.DrawImage(bmp, Gdiplus::Rect(0,0,w,h), 0,0,bmp->GetWidth(),bmp->GetHeight(), Gdiplus::UnitPixel);
    HBITMAP hb = nullptr;
    scaled->GetHBITMAP(Gdiplus::Color(0,0,0,0), &hb);
    delete bmp; delete scaled;
    return hb;
}
// robust flag lookup: next to the exe first, then walk up the tree looking for
// the source tree - flags are always shipped inside the "flags" folder.
std::wstring flagPathForCode(const std::string& code) {
    wchar_t exe[MAX_PATH]; GetModuleFileNameW(nullptr, exe, MAX_PATH);
    std::wstring exeDir = exe;
    size_t p = exeDir.find_last_of(L"\\/");
    if (p != std::wstring::npos) exeDir = exeDir.substr(0, p);
    std::wstring cur = exeDir;
    for (int i = 0; i < 6; ++i) {
        std::wstring cand = cur + L"\\flags\\" + fromUtf8(code) + L".png";
        if (fileExists(cand)) return cand;
        cand = cur + L"\\src\\main\\cpp\\launcher\\windows\\flags\\" + fromUtf8(code) + L".png";
        if (fileExists(cand)) return cand;
        size_t pos = cur.find_last_of(L"\\/");
        if (pos == std::wstring::npos) break;
        cur = cur.substr(0, pos);
        if (cur.empty()) break;
    }
    return L"";
}
// --- install-path drive icon (Win10 "Local Disk" style) --------------------
std::wstring setupIconPath(const std::wstring& name) {
    wchar_t exe[MAX_PATH]; GetModuleFileNameW(nullptr, exe, MAX_PATH);
    // keep the trailing separator - dropping it made every concatenation produce
    // "...launcher-checkicons\..." which never matched, so the themed logo was
    // silently skipped and the default file (found via the source-tree walk)
    // was used for every theme.
    std::wstring s = exe; size_t p = s.find_last_of(L"\\/"); if (p != std::wstring::npos) s = s.substr(0, p + 1);
    if (!themeIconSubdir().empty()) {
        std::wstring themed = s + L"icons\\" + themeIconSubdir() + name + L".png";
        if (fileExists(themed)) return themed;
    }
    std::wstring cand = s + L"icons\\" + name + L".png";
    if (fileExists(cand)) return cand;
    std::wstring cur = s;
    for (int i = 0; i < 5; ++i) {
        size_t pos = cur.find_last_of(L"\\/");
        if (pos == std::wstring::npos) break;
        cur = cur.substr(0, pos);
        if (cur.empty()) break;
        cand = cur + L"icons\\" + name + L".png";
        if (fileExists(cand)) return cand;
        cand = cur + L"\\src\\main\\cpp\\launcher\\windows\\icons\\" + name + L".png";
        if (fileExists(cand)) return cand;
    }
    return L"";
}
HICON loadPngIcon(const std::wstring& path, int size) {
    if (path.empty()) return nullptr;
    Gdiplus::Bitmap* src = Gdiplus::Bitmap::FromFile(path.c_str());
    if (!src || src->GetLastStatus() != Gdiplus::Ok) { delete src; return nullptr; }
    Gdiplus::Bitmap dst(size, size, PixelFormat32bppARGB);
    Gdiplus::Graphics g(&dst);
    g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
    g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
    g.SetPixelOffsetMode(Gdiplus::PixelOffsetModeHighQuality);
    g.SetCompositingQuality(Gdiplus::CompositingQualityHighQuality);
    g.Clear(Gdiplus::Color(0,0,0,0));
    g.DrawImage(src, Gdiplus::Rect(0,0,size,size), 0,0,src->GetWidth(),src->GetHeight(), Gdiplus::UnitPixel);
    HICON out = nullptr;
    dst.GetHICON(&out);
    delete src;
    return out;
}
// (Re)loads the Windows logo for the active theme: the themed icon set wins
// (real Win95 / Win7 flag for those themes), otherwise the default monochrome
// logo is recoloured white on dark themes / black on light ones.
void reloadSetupLogo() {
    if (g_setup.winIconBmp) { delete g_setup.winIconBmp; g_setup.winIconBmp = nullptr; }
    std::wstring cand = setupIconPath(L"win10");  // theme folder first, then default
    bool themed = isThemedIconPath(cand);
    if (!cand.empty() && fileExists(cand)) g_setup.winIconBmp = Gdiplus::Bitmap::FromFile(cand.c_str());
    if (g_setup.winIconBmp && g_setup.winIconBmp->GetLastStatus() != Gdiplus::Ok) { delete g_setup.winIconBmp; g_setup.winIconBmp = nullptr; }
    if (!g_setup.winIconBmp || themed) return;   // themed flags keep their colours
    // force the monochrome colour instead of hoping the source already matches.
    BYTE target = g_setup.theme.isDark ? 255 : 18;
    Gdiplus::Rect rc(0, 0, g_setup.winIconBmp->GetWidth(), g_setup.winIconBmp->GetHeight());
    Gdiplus::BitmapData d;
    if (g_setup.winIconBmp->LockBits(&rc, Gdiplus::ImageLockModeRead | Gdiplus::ImageLockModeWrite, PixelFormat32bppARGB, &d) == Gdiplus::Ok) {
        for (UINT y = 0; y < d.Height; ++y) {
            BYTE* row = (BYTE*)d.Scan0 + y * d.Stride;
            for (UINT x = 0; x < d.Width; ++x) if (row[x*4+3]) { row[x*4+0] = target; row[x*4+1] = target; row[x*4+2] = target; }
        }
        g_setup.winIconBmp->UnlockBits(&d);
    }
}
// --- live theme switching inside Setup -------------------------------------
void applySetupTheme(const std::string& name) {
    LauncherConfig lc = loadLauncherConfig();
    lc.theme = name;
    saveLauncherConfig(lc);              // applied automatically on first launcher start
    setThemeIconSubdir(themeIconSubdirFor(name));
    g_setup.theme = getThemeForConfig(name, lc.customBg, lc.customPanel, lc.customText, lc.customAccent, lc.customButton, lc.customAlpha, lc.customGlow, lc.glowEnabled);
    // same normalisation as at startup, but only for the built-in themes.
    if (name == "dark") { g_setup.theme.bg = RGB(18,18,18); g_setup.theme.panel = RGB(30,30,30); }
    else if (name == "light") { g_setup.theme.bg = RGB(232,232,235); g_setup.theme.panel = RGB(248,248,250); g_setup.theme.text = RGB(18,18,20); }
    // glow in Setup is exclusive to the Dark theme.
    g_setup.theme.glowEnabled = (name == "dark");
    if (g_setup.theme.isDark) g_setup.theme.glow = RGB(255,255,255);
    if (g_setup.bgBrush) DeleteObject(g_setup.bgBrush);
    g_setup.bgBrush = CreateSolidBrush(g_setup.theme.bg);
    reloadSetupLogo();  // the Windows logo follows the theme: white / black
    applyWindowTheme(g_setup.hwnd, g_setup.theme);
    // ERASE included: every child/static must repaint over the old colour.
    RedrawWindow(g_setup.hwnd, nullptr, nullptr, RDW_INVALIDATE | RDW_ERASE | RDW_ALLCHILDREN | RDW_UPDATENOW | RDW_FRAME);
}
void updateLangTexts() {
    SetWindowTextW(g_setup.hwnd, fromUtf8(lang("setup_title")).c_str());
    SetWindowTextW(g_setup.hTitle, fromUtf8(lang("setup_title")).c_str());
    std::wstring welcome = fromUtf8(std::string(lang("setup_welcome")) + "\r\n" + lang("setup_welcome2"));
    SetWindowTextW(g_setup.hWelcome, welcome.c_str());
    SetWindowTextW(g_setup.hLangLabel, fromUtf8(lang("setup_lang")).c_str());
    {
        const char* tv = lang("theme");
        if (!tv || strcmp(tv, "theme") == 0) tv = "Theme";
        SetWindowTextW(g_setup.hThemeLabel, fromUtf8(tv).c_str());
    }
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
static constexpr float kWin10Cycle = 4.8f;
static constexpr int kWin10Dots = 5;
static constexpr float kWin10Delay = 0.24f;
static constexpr float kWin10Radius = 18.0f;
static constexpr float kWin10DotR = 2.5f;
static constexpr float kWin10PI = 3.14159265f;
static constexpr float kWin10FadeIn = 0.05f;
static constexpr float kWin10FadeOut = 0.03f;
static constexpr int kWin10LUTSize = 256;
static float kWin10LUT[kWin10LUTSize];
void initWin10LUT() {
    for (int i = 0; i < kWin10LUTSize; ++i) {
        float t = (float)i / (kWin10LUTSize - 1);
        float angle;
        if (t < 0.07f) {
            float p = t / 0.07f;
            p = p * p * (3.0f - 2.0f * p);
            angle = 225.0f + (345.0f - 225.0f) * p;
        } else if (t < 0.30f) {
            float p = (t - 0.07f) / (0.30f - 0.07f);
            angle = 345.0f + (455.0f - 345.0f) * p;
        } else if (t < 0.39f) {
            float p = (t - 0.30f) / (0.39f - 0.30f);
            p = p * p * (3.0f - 2.0f * p);
            angle = 455.0f + (690.0f - 455.0f) * p;
        } else if (t < 0.70f) {
            float p = (t - 0.39f) / (0.70f - 0.39f);
            angle = 690.0f + (815.0f - 690.0f) * p;
        } else if (t < 0.75f) {
            float p = (t - 0.70f) / (0.75f - 0.70f);
            p = p * p * (3.0f - 2.0f * p);
            angle = 815.0f + (945.0f - 815.0f) * p;
        } else {
            angle = 945.0f;
        }
        kWin10LUT[i] = angle * kWin10PI / 180.0f;
    }
}
float win10EvalLUT(float t) {
    if (t <= 0) return kWin10LUT[0];
    if (t >= 1.0f) return kWin10LUT[kWin10LUTSize - 1];
    float fi = t * (kWin10LUTSize - 1);
    int idx = (int)fi;
    float frac = fi - idx;
    if (idx >= kWin10LUTSize - 1) return kWin10LUT[kWin10LUTSize - 1];
    return kWin10LUT[idx] + (kWin10LUT[idx + 1] - kWin10LUT[idx]) * frac;
}
void drawSpinner(HDC hdc, int cx, int cy, float phase) {
    Gdiplus::Graphics g(hdc);
    g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
    for (int i = 0; i < kWin10Dots; ++i) {
        float dotPhase = fmod(phase - i * kWin10Delay, kWin10Cycle);
        if (dotPhase < 0) dotPhase += kWin10Cycle;
        float t = dotPhase / kWin10Cycle;
        if (t >= 0.76f) continue;
        float alpha = 1.0f;
        if (t < kWin10FadeIn) alpha = t / kWin10FadeIn;
        else if (t > 0.76f - kWin10FadeOut) alpha = (0.76f - t) / kWin10FadeOut;
        float angleRad = win10EvalLUT(t);
        float px = cx + sinf(angleRad) * kWin10Radius;
        float py = cy - cosf(angleRad) * kWin10Radius;
        BYTE a = (BYTE)(alpha * 255);
        Gdiplus::SolidBrush br(Gdiplus::Color(a, 255, 255, 255));
        g.FillEllipse(&br, px - kWin10DotR, py - kWin10DotR, kWin10DotR * 2.0f, kWin10DotR * 2.0f);
    }
}
// controls that belong to a page's content area and take part in the slide
// transition. Navigation buttons, spinner and details panel are "chrome" and
// switch instantly.
std::vector<HWND> pageContent(int page) {
    std::vector<HWND> v;
    switch (page) {
        case 0: v = {g_setup.hLangLabel, g_setup.hLangCombo, g_setup.hThemeLabel, g_setup.hThemeCombo}; break;
        case 1: v = {g_setup.hWelcome}; break;
        case 2: v = {GetDlgItem(g_setup.hwnd, 3001), g_setup.hPath, g_setup.hBrowse, g_setup.hDiskIcon, g_setup.hDiskInfo, g_setup.hDiskBar}; break;
        case 3: v = {GetDlgItem(g_setup.hwnd, 3002), g_setup.hStartMenu}; break;
        case 4: v = {g_setup.hShortcutDesktop, g_setup.hShortcutStart, g_setup.hAutoStart, g_setup.hAutoLaunch}; break;
        case 5: v = {g_setup.hSummary}; break;
        case 6: v = {g_setup.hStatus, g_setup.hDetailsBtn}; break;
        default: break;
    }
    v.erase(std::remove_if(v.begin(), v.end(), [](HWND h) { return !h || !IsWindow(h); }), v.end());
    return v;
}

// --- Custom palette (shared with the launcher: the same launcher.json fields)
void loadPaletteColors() {
    LauncherConfig lc = loadLauncherConfig();
    g_setup.palColors[0] = lc.customBg;
    g_setup.palColors[1] = lc.customPanel;
    g_setup.palColors[2] = lc.customText;
    g_setup.palColors[3] = lc.customAccent;
    g_setup.palColors[4] = lc.customButton ? lc.customButton : lc.customAccent;
    g_setup.palColors[5] = lc.customGlow;
}
// palette swatches are visible only when "Custom" is selected on the Language page.
void updatePaletteVisibility() {
    loadPaletteColors();  // stay in sync with launcher.json
    bool show = g_setup.themeIsCustom && g_setup.page == 0;
    int cmd = show ? SW_SHOW : SW_HIDE;
    if (g_setup.hPaletteLabel) ShowWindow(g_setup.hPaletteLabel, cmd);
    for (HWND b : g_setup.hColorBtns) {
        if (b) { ShowWindow(b, cmd); if (show) InvalidateRect(b, nullptr, TRUE); }
    }
}

// everything that switches instantly on a page change (step indicator state,
// navigation buttons, summary text, spinner timer).
void showPageChrome(int page) {
    g_setup.page = page;
    ShowWindow(g_setup.hProgress, SW_HIDE);
    ShowWindow(g_setup.hDetailsEdit, (page == 6 && g_setup.detailsVisible) ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hInstall, page == 5 ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hNext, (page >= 0 && page < 5) ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hBack, (page > 0 && page < 6) ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hCancelSetup, (page == 6 && g_setup.busy) ? SW_SHOW : SW_HIDE);
    ShowWindow(g_setup.hWinIcon, SW_SHOW);
    if (page == 5) {
        wchar_t path[MAX_PATH]; GetWindowTextW(g_setup.hPath, path, MAX_PATH);
        wchar_t menu[MAX_PATH]; GetWindowTextW(g_setup.hStartMenu, menu, MAX_PATH);
        std::wstring sum = fromUtf8(lang("setup_summary_to")) + std::wstring(path) + L"\r\n" + fromUtf8(lang("setup_summary_menu")) + std::wstring(menu);
        SetWindowTextW(g_setup.hSummary, sum.c_str());
    }
    if (page == 6) SetTimer(g_setup.hwnd, 50, 16, nullptr); else KillTimer(g_setup.hwnd, 50);
    EnableWindow(g_setup.hNext, TRUE);
    RECT wrc; GetClientRect(g_setup.hwnd, &wrc);
    RECT stepRc = {0, 0, wrc.right, 90};
    InvalidateRect(g_setup.hwnd, &stepRc, TRUE);
    InvalidateRect(g_setup.hwnd, nullptr, TRUE);
    updatePaletteVisibility();
}

void showPageImmediate(int page) {
    KillTimer(g_setup.hwnd, TIMER_PAGE_ANIM);
    g_setup.animActive = false;
    g_setup.animOut.clear(); g_setup.animIn.clear();
    g_setup.animOutP0.clear(); g_setup.animInP0.clear();
    showPageChrome(page);
    for (int i = 0; i <= 6; ++i) {
        for (HWND h : pageContent(i)) ShowWindow(h, i == page ? SW_SHOW : SW_HIDE);
    }
}

bool captureClientPos(HWND h, POINT& p) {
    if (!h || !IsWindow(h)) return false;
    RECT r;
    if (!GetWindowRect(h, &r)) return false;
    p.x = r.left; p.y = r.top;
    ScreenToClient(g_setup.hwnd, &p);
    return true;
}

// snap any running transition to its final state.
void finishPageAnim() {
    if (!g_setup.animActive) return;
    KillTimer(g_setup.hwnd, TIMER_PAGE_ANIM);
    // let the canonical layout re-place everything, then settle visibility.
    RECT rc; GetClientRect(g_setup.hwnd, &rc);
    SendMessageW(g_setup.hwnd, WM_SIZE, 0, MAKELPARAM(rc.right, rc.bottom));
    for (HWND h : g_setup.animIn) if (IsWindow(h)) SetLayeredWindowAttributes(h, 0, 255, LWA_ALPHA);
    for (HWND h : g_setup.animOut) if (IsWindow(h)) { ShowWindow(h, SW_HIDE); SetLayeredWindowAttributes(h, 0, 255, LWA_ALPHA); }
    // drop the WS_EX_LAYERED style again: controls left as layered windows
    // repaint through a composited surface, which smudged buttons/combos on
    // later presses. Plain GDI children render crisply.
    auto stripLayered = [](HWND h) {
        if (!h || !IsWindow(h)) return;
        LONG_PTR ex = GetWindowLongPtrW(h, GWL_EXSTYLE);
        if (ex & WS_EX_LAYERED) {
            SetWindowLongPtrW(h, GWL_EXSTYLE, ex & ~static_cast<LONG_PTR>(WS_EX_LAYERED));
            SetWindowPos(h, nullptr, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE | SWP_FRAMECHANGED);
        }
    };
    for (HWND h : g_setup.animIn) stripLayered(h);
    for (HWND h : g_setup.animOut) stripLayered(h);
    g_setup.animOut.clear(); g_setup.animIn.clear();
    g_setup.animOutP0.clear(); g_setup.animInP0.clear();
    g_setup.animActive = false;
    EnableWindow(g_setup.hNext, TRUE);
    EnableWindow(g_setup.hBack, TRUE);
}

void doFadeTo(int target) {
    if (target == g_setup.page && !g_setup.animActive) return;
    finishPageAnim();               // complete a previous flight instantly
    if (target == g_setup.page) return;
    std::vector<HWND> outC = pageContent(g_setup.page);
    std::vector<HWND> inC = pageContent(target);
    showPageChrome(target);         // header + navigation switch right away
    bool layeredOk = false;
    POINT p{};
    for (HWND h : inC) {
        if (!captureClientPos(h, p)) continue;
        g_setup.animIn.push_back(h);
        g_setup.animInP0.push_back(p);
        SetWindowPos(h, nullptr, p.x, p.y - PAGE_SLIDE, 0, 0, SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
        ShowWindow(h, SW_SHOWNA);
        LONG_PTR ex = GetWindowLongPtrW(h, GWL_EXSTYLE);
        SetWindowLongPtrW(h, GWL_EXSTYLE, ex | WS_EX_LAYERED);
        if (SetLayeredWindowAttributes(h, 0, 0, LWA_ALPHA)) layeredOk = true;
    }
    for (HWND h : outC) {
        if (!captureClientPos(h, p)) continue;
        g_setup.animOut.push_back(h);
        g_setup.animOutP0.push_back(p);
        LONG_PTR ex = GetWindowLongPtrW(h, GWL_EXSTYLE);
        SetWindowLongPtrW(h, GWL_EXSTYLE, ex | WS_EX_LAYERED);
        SetLayeredWindowAttributes(h, 0, 255, LWA_ALPHA);
    }
    if (!layeredOk) {
        // per-child alpha is unsupported (Windows 7) - fall back to an instant
        // switch, exactly like the launcher behaved before the animation.
        finishPageAnim();
        return;
    }
    g_setup.animActive = true;
    g_setup.animT = 0.0f;
    EnableWindow(g_setup.hNext, FALSE);
    EnableWindow(g_setup.hBack, FALSE);
    SetTimer(g_setup.hwnd, TIMER_PAGE_ANIM, 10, nullptr);
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
bool isUserAdmin() {
    BOOL admin = FALSE;
    PSID grp = nullptr;
    SID_IDENTIFIER_AUTHORITY auth = SECURITY_NT_AUTHORITY;
    if (AllocateAndInitializeSid(&auth, 2, SECURITY_BUILTIN_DOMAIN_RID, DOMAIN_ALIAS_RID_ADMINS, 0,0,0,0,0,0, &grp)) {
        CheckTokenMembership(nullptr, grp, &admin);
        FreeSid(grp);
    }
    return admin != FALSE;
}
bool isProtectedPath(const std::wstring& p) {
    std::wstring l = p;
    for (auto &c : l) c = towlower(c);
    return l.find(L"\\program files") != std::wstring::npos || l.find(L"\\windows\\") != std::wstring::npos;
}
bool isDirEmpty(const std::wstring& dir) {
    WIN32_FIND_DATAW fd;
    HANDLE h = FindFirstFileW(joinPath(dir, L"*").c_str(), &fd);
    if (h == INVALID_HANDLE_VALUE) return true;
    bool empty = true;
    do {
        std::wstring n = fd.cFileName;
        if (n != L"." && n != L"..") { empty = false; break; }
    } while (FindNextFileW(h, &fd));
    FindClose(h);
    return empty;
}
bool validateInstallPath(const std::wstring& p, std::string& err) {
    if (p.empty()) { err = lang("setup_select_folder"); return false; }
    if (p.size() > 240) { err = "Path too long"; return false; }
    const std::wstring invalid = L"<>\"|?*";
    for (size_t i=0;i<p.size();++i) { wchar_t c=p[i]; if (c==L':' && i==1) continue; if (invalid.find(c) != std::wstring::npos) { err = "Invalid characters in path"; return false; } if (c==L':' && i!=1) { err = "Invalid characters in path"; return false; } }
    if (p.size() >= 2 && p[1] != L':' && p[0] != L'\\') { err = "Invalid path"; return false; }
    ULARGE_INTEGER freeBytes; ULARGE_INTEGER total;
    std::wstring root = p.substr(0, p.find_first_of(L"\\/")+1);
    if (p.size() >= 2 && p[1]==L':') root = p.substr(0,3);
    if (GetDiskFreeSpaceExW(root.c_str(), &freeBytes, &total, nullptr)) {
        if (freeBytes.QuadPart < 500 * 1024 * 1024) { err = "Not enough disk space (need 500MB)"; return false; }
    }
    std::wstring trimmed = p;
    while (!trimmed.empty() && (trimmed.back() == L'\\' || trimmed.back() == L'/')) trimmed.pop_back();
    if (trimmed.size() >= 2 && trimmed.size() <= 3 && trimmed[1] == L':') { err = "Cannot install to a drive root"; return false; }
    if (fileExists(trimmed)) {
        bool ourInstall = fileExists(joinPath(trimmed, L"kickx_launcher.exe")) || fileExists(joinPath(trimmed, L"KickXSetup.exe"));
        if (!ourInstall && !isDirEmpty(trimmed)) { err = "Folder is not empty - select a new folder"; return false; }
    }
    return true;
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
void updateDiskInfo() {
    wchar_t pathBuf[MAX_PATH]; GetWindowTextW(g_setup.hPath, pathBuf, MAX_PATH);
    std::wstring path = pathBuf;
    if (path.empty() || (path.size() < 2 || path[1] != L':')) {
        SetWindowTextW(g_setup.hDiskInfo, L"");
        SendMessageW(g_setup.hDiskBar, PBM_SETPOS, 0, 0);
        return;
    }
    std::wstring root = path.substr(0, 3);
    ULARGE_INTEGER freeBytes, totalBytes;
    if (GetDiskFreeSpaceExW(root.c_str(), &freeBytes, &totalBytes, nullptr)) {
        double freeGB = (double)freeBytes.QuadPart / (1024.0*1024*1024);
        double totalGB = (double)totalBytes.QuadPart / (1024.0*1024*1024);
        double usedPct = (totalGB > 0) ? ((totalGB - freeGB) / totalGB * 100.0) : 0;
        wchar_t buf[128]; swprintf_s(buf, L"%.1f GB / %.1f GB (%.0f%% free)", freeGB, totalGB, 100.0 - usedPct);
        SetWindowTextW(g_setup.hDiskInfo, buf);
        SendMessageW(g_setup.hDiskBar, PBM_SETPOS, (WPARAM)(int)(usedPct), 0);
    } else {
        SetWindowTextW(g_setup.hDiskInfo, L"");
        SendMessageW(g_setup.hDiskBar, PBM_SETPOS, 0, 0);
    }
}
void workerSetup() {
    g_setup.busy = true;
    g_setup.cancelRequested = false;
    g_setup.detailsLog.clear();
    if (g_setup.hDetailsEdit) SetWindowTextW(g_setup.hDetailsEdit, L"");
    EnableWindow(g_setup.hInstall, FALSE);
    wchar_t pathBuf[MAX_PATH]; GetWindowTextW(g_setup.hPath, pathBuf, MAX_PATH);
    std::wstring installDir = pathBuf;
    if (installDir.empty()) { setStatusDetail(lang("setup_select_folder")); EnableWindow(g_setup.hInstall, TRUE); g_setup.busy = false; return; }
    {
        LauncherConfig lcSave = loadLauncherConfig();
        std::string narrowDir = toUtf8(installDir);
        auto it = std::find(lcSave.installHistory.begin(), lcSave.installHistory.end(), narrowDir);
        if (it != lcSave.installHistory.end()) lcSave.installHistory.erase(it);
        lcSave.installHistory.push_back(narrowDir);
        if (lcSave.installHistory.size() > 5) lcSave.installHistory.erase(lcSave.installHistory.begin(), lcSave.installHistory.begin() + (lcSave.installHistory.size() - 5));
        lcSave.autoStart = g_setup.autoStartChecked;
        saveLauncherConfig(lcSave);
    }
    if (isProtectedPath(installDir) && !isUserAdmin()) {
        std::string msg = std::string(lang("setup_select_folder")) + " - " + lang("setup_admin_required");
        setStatusDetail(msg);
        appendDetail("ERROR: Admin required for: " + toUtf8(installDir));
        PostMessageW(g_setup.hwnd, WM_APP+12, 0,0);
        MessageBoxW(g_setup.hwnd, fromUtf8(msg + "\n" + lang("setup_admin_hint")).c_str(), fromUtf8(lang("setup_title")).c_str(), MB_OK | MB_ICONWARNING);
        PostMessageW(g_setup.hwnd, WM_APP+12, 0,0);
        EnableWindow(g_setup.hInstall, TRUE); g_setup.busy = false; return;
    }
    createDirs(installDir);
    if (g_setup.cancelRequested) { g_setup.busy=false; return; }
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
        PostMessageW(g_setup.hwnd, WM_APP+12, 0,0);
        MessageBoxW(g_setup.hwnd, fromUtf8(std::string(lang("setup_launcher_missing")) + "\n" + toUtf8(srcLauncher) + "\n\n" + lang("setup_extract_hint")).c_str(), fromUtf8(lang("setup_title")).c_str(), MB_OK | MB_ICONERROR);
        PostMessageW(g_setup.hwnd, WM_APP+12, 0,0);
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
    writeFileAtomic(joinPath(installDir, L"kickx_manifest.txt"), "kickx_launcher.exe\nKickXSetup.exe\nicons\nflags\n");
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
                if (g_setup.taskbar && g_setup.taskbarReady) {
                    g_setup.taskbar->SetProgressValue(g_setup.hwnd, pct, 100);
                }
            }
        }
    };
        setStatusDetail(std::string(lang("setup_checking_java")) + " 21...");
    if (g_setup.cancelRequested) { g_setup.busy=false; return; }
    std::wstring javaPath;
    if (!ravex::game::ensureJava(21, javaPath, &err, [](const std::string& s){ PostMessageW(g_setup.hwnd, WM_APP + 10, 0, (LPARAM)new std::string(s)); }, nullptr)) {
        setStatusDetail(std::string(lang("setup_checking_java")) + " fail: " + err);
        PostMessageW(g_setup.hwnd, WM_APP+12, 0,0);
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
        if (g_setup.cancelRequested) { g_setup.busy=false; return; }
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
                PostMessageW(g_setup.hwnd, WM_APP+12, 0,0);
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
    // autostart: put a visible shortcut into the user's Startup folder so the
    // launcher runs on logon. Not elevated, not hidden - the user can always see
    // and delete it (shell:startup), which is exactly what an honest autostart is.
    {
        PWSTR pStartup = nullptr;
        if (SHGetKnownFolderPath(FOLDERID_Startup, 0, nullptr, &pStartup) == S_OK) {
            std::wstring link = std::wstring(pStartup) + L"\\KickX.lnk";
            if (g_setup.autoStartChecked) {
                createShortcut(dstLauncher, link, fromUtf8(lang("setup_title")).c_str());
                appendDetail(fileExists(link) ? "Autostart shortcut created" : "Autostart shortcut FAILED");
            } else {
                DeleteFileW(link.c_str());
                appendDetail("Autostart shortcut removed (option disabled)");
            }
            CoTaskMemFree(pStartup);
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
    static UINT s_taskbarMsg = 0;
    if (!s_taskbarMsg && msg != WM_CREATE) s_taskbarMsg = RegisterWindowMessageW(L"TaskbarButtonCreated");
    if (s_taskbarMsg && msg == s_taskbarMsg) {
        if (!g_setup.taskbar) {
            CoCreateInstance(CLSID_TaskbarList, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&g_setup.taskbar));
            if (g_setup.taskbar) g_setup.taskbar->HrInit();
        }
        g_setup.taskbarReady = true;
        return 0;
    }
    switch (msg) {
        case WM_CREATE: {
            g_setup.hwnd = hwnd;
            {
                LauncherConfig lc0 = loadLauncherConfig();
                g_setup.theme = getThemeForConfig(lc0.theme, lc0.customBg, lc0.customPanel, lc0.customText, lc0.customAccent, lc0.customButton, lc0.customAlpha, lc0.customGlow, lc0.glowEnabled);
                // normalise only the built-in themes - Win95 / Vista keep their palette.
                if (lc0.theme == "dark") { g_setup.theme.bg = RGB(18,18,18); g_setup.theme.panel = RGB(30,30,30); }
                else if (lc0.theme == "light") { g_setup.theme.bg = RGB(232,232,235); g_setup.theme.panel = RGB(248,248,250); g_setup.theme.text = RGB(18,18,20); }
                // glow in Setup is exclusive to the Dark theme.
                g_setup.theme.glowEnabled = (lc0.theme == "dark");
                if (g_setup.theme.isDark) g_setup.theme.glow = RGB(255,255,255);
            }
            g_setup.bgBrush = CreateSolidBrush(g_setup.theme.bg);
            g_setup.font = makeSetupFont();
            g_setup.titleFont = makeTitleFont();
            HINSTANCE inst = (HINSTANCE)GetWindowLongPtrW(hwnd, GWLP_HINSTANCE);
                    g_setup.hTitle = CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_title")).c_str(), WS_CHILD | WS_VISIBLE | SS_CENTER, 0, 10, 560, 26, hwnd, nullptr, inst, nullptr);
            g_setup.hWinIcon = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_OWNERDRAW, 12, 320, 44, 44, hwnd, nullptr, inst, nullptr);
            reloadSetupLogo();  // default Windows logo, coloured for the active theme
            g_setup.hLangLabel = CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_lang")).c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 50, 500, 22, hwnd, nullptr, inst, nullptr);
            g_setup.hLangCombo = CreateWindowExW(0, L"COMBOBOX", nullptr, WS_CHILD | WS_VISIBLE | CBS_OWNERDRAWFIXED | CBS_DROPDOWNLIST | CBS_HASSTRINGS | WS_VSCROLL, 20, 74, 300, 200, hwnd, (HMENU)IDC_LANG, inst, nullptr);
            for (int i = 0; i < langCount(); ++i) {
                const char* code = langCodeByIndex(i);
                const char* name = langDisplayName(code);
                std::wstring w = fromUtf8(name);
                SendMessageW(g_setup.hLangCombo, CB_ADDSTRING, 0, (LPARAM)w.c_str());
                std::wstring fp = flagPathForCode(code);
                g_setup.flagBmps.push_back(fp.empty() ? nullptr : loadFlagBmp(fp));
            }
            SendMessageW(g_setup.hLangCombo, CB_SETCURSEL, 0, 0);
            {
                LauncherConfig lc = loadLauncherConfig();
                for (int i = 0; i < langCount(); ++i) if (std::string(langCodeByIndex(i)) == lc.language) { SendMessageW(g_setup.hLangCombo, CB_SETCURSEL, i, 0); setCurrentLanguage(langCodeByIndex(i)); break; }
            }
            {
                const char* tv = lang("theme");
                if (!tv || strcmp(tv, "theme") == 0) tv = "Theme";
                g_setup.hThemeLabel = CreateWindowExW(0, L"STATIC", fromUtf8(tv).c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 108, 500, 22, hwnd, nullptr, inst, nullptr);
            }
            g_setup.hThemeCombo = CreateWindowExW(0, L"COMBOBOX", nullptr, WS_CHILD | WS_VISIBLE | CBS_DROPDOWNLIST | WS_VSCROLL, 20, 132, 300, 200, hwnd, (HMENU)IDC_THEME_SETUP, inst, nullptr);
            {
                SendMessageW(g_setup.hThemeCombo, CB_ADDSTRING, 0, (LPARAM)L"Dark");
                SendMessageW(g_setup.hThemeCombo, CB_ADDSTRING, 0, (LPARAM)L"Light");
                SendMessageW(g_setup.hThemeCombo, CB_ADDSTRING, 0, (LPARAM)L"Windows 95");
                SendMessageW(g_setup.hThemeCombo, CB_ADDSTRING, 0, (LPARAM)L"Windows Vista / 7");
                SendMessageW(g_setup.hThemeCombo, CB_ADDSTRING, 0, (LPARAM)L"Custom");
                LauncherConfig lcT = loadLauncherConfig();
                int idxT = 4;  // palette themes (midnight/ocean/...) fall under Custom
                if (lcT.theme == "dark") idxT = 0;
                else if (lcT.theme == "light") idxT = 1;
                else if (lcT.theme == "win95") idxT = 2;
                else if (lcT.theme == "vista") idxT = 3;
                SendMessageW(g_setup.hThemeCombo, CB_SETCURSEL, idxT, 0);
                g_setup.themeIsCustom = (idxT == 4);
            }
            {
                auto trS = [&](const char* k, const char* fb) -> std::wstring {
                    const char* v = lang(k); if (!v || strcmp(v, k) == 0) v = fb;
                    return fromUtf8(v);
                };
                g_setup.hPaletteLabel = CreateWindowExW(0, L"STATIC", trS("palette", "Palette (for Custom)").c_str(), WS_CHILD | SS_LEFT, 20, 170, 400, 18, hwnd, nullptr, inst, nullptr);
                const char* keys[6] = {"background", "panel_label", "text_label", "accent_label", "button_label", "glow_color"};
                const char* fbs[6] = {"Background", "Panel", "Text", "Accent", "Button", "Glow"};
                const int xs[3] = {20, 150, 280};
                for (int i = 0; i < 6; ++i) {
                    int x = xs[i % 3];
                    int y = (i < 3) ? 196 : 228;
                    g_setup.hColorBtns[i] = CreateWindowExW(0, L"BUTTON", trS(keys[i], fbs[i]).c_str(), WS_CHILD | BS_OWNERDRAW, x, y, 120, 26, hwnd, (HMENU)(IDC_COLOR_BG + i), inst, nullptr);
                }
                SendMessageW(g_setup.hPaletteLabel, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
                for (HWND b : g_setup.hColorBtns) if (b) SendMessageW(b, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            }
            g_setup.hWelcome = CreateWindowExW(0, L"STATIC", fromUtf8(std::string(lang("setup_welcome")) + "\r\n" + lang("setup_welcome2")).c_str(), WS_CHILD | WS_VISIBLE | SS_CENTER, 20, 55, 520, 40, hwnd, nullptr, inst, nullptr);
            CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_dest")).c_str(), WS_CHILD | WS_VISIBLE, 20, 55, 520, 22, hwnd, (HMENU)3001, inst, nullptr);
            g_setup.hPath = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 20, 80, 380, 24, hwnd, (HMENU)IDC_PATH, inst, nullptr);
            g_setup.hBrowse = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_browse")).c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 410, 80, 90, 24, hwnd, (HMENU)IDC_BROWSE, inst, nullptr);
            g_setup.hDiskIcon = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 20, 78, 24, 24, hwnd, nullptr, inst, nullptr);
            g_setup.hDiskIco = loadPngIcon(setupIconPath(L"disk"), 24);
            if (g_setup.hDiskIco) SendMessageW(g_setup.hDiskIcon, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(g_setup.hDiskIco));
            g_setup.hDiskInfo = CreateWindowExW(0, L"STATIC", L"", WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 110, 300, 16, hwnd, (HMENU)IDC_DISK_INFO, inst, nullptr);
            g_setup.hDiskBar = CreateWindowExW(0, PROGRESS_CLASSW, nullptr, WS_CHILD | WS_VISIBLE, 330, 110, 160, 14, hwnd, (HMENU)IDC_DISK_BAR, inst, nullptr);
            CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_start")).c_str(), WS_CHILD | WS_VISIBLE, 20, 55, 520, 22, hwnd, (HMENU)3002, inst, nullptr);
            g_setup.hStartMenu = CreateWindowExW(0, L"EDIT", L"KickX", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 20, 80, 520, 24, hwnd, (HMENU)IDC_STARTMENU, inst, nullptr);
            g_setup.hShortcutDesktop = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_shortcut_desktop")).c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 20, 65, 260, 28, hwnd, (HMENU)IDC_SHORTCUT_DESKTOP, inst, nullptr);
            g_setup.hShortcutStart = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_shortcut_start")).c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 20, 100, 280, 28, hwnd, (HMENU)IDC_SHORTCUT_START, inst, nullptr);
            g_setup.hAutoStart = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_autostart")).c_str(), WS_CHILD | BS_OWNERDRAW, 20, 135, 280, 28, hwnd, (HMENU)IDC_AUTOSTART, inst, nullptr);
            g_setup.hAutoLaunch = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_autolaunch")).c_str(), WS_CHILD | BS_OWNERDRAW, 20, 170, 280, 28, hwnd, (HMENU)IDC_AUTOLAUNCH, inst, nullptr);
            g_setup.hSummary = CreateWindowExW(0, L"STATIC", L"", WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 36, 500, 60, hwnd, nullptr, inst, nullptr);
            SendMessageW(g_setup.hShortcutDesktop, BM_SETCHECK, BST_CHECKED, 0);
            SendMessageW(g_setup.hShortcutStart, BM_SETCHECK, BST_CHECKED, 0);
            PWSTR pLocal = nullptr; if (SHGetKnownFolderPath(FOLDERID_LocalAppData, 0, nullptr, &pLocal) == S_OK) { std::wstring def = std::wstring(pLocal) + L"\\KickX"; SetWindowTextW(g_setup.hPath, def.c_str()); CoTaskMemFree(pLocal); } else SetWindowTextW(g_setup.hPath, L"C:\\KickX");
            {
                LauncherConfig lcHist = loadLauncherConfig();
                if (!lcHist.installHistory.empty()) {
                    std::wstring last = fromUtf8(lcHist.installHistory.back());
                    SetWindowTextW(g_setup.hPath, last.c_str());
                }
                g_setup.autoStartChecked = lcHist.autoStart;
                g_setup.chkTarget[2] = g_setup.autoStartChecked;
                g_setup.chkAnim[2] = g_setup.autoStartChecked ? 1.0f : 0.0f;
            }
            g_setup.hStatus = CreateWindowExW(0, L"STATIC", fromUtf8(lang("setup_ready_install")).c_str(), WS_CHILD | WS_VISIBLE | SS_CENTER, 0, 98, 540, 16, hwnd, nullptr, inst, nullptr);
            g_setup.hProgress = CreateWindowExW(0, PROGRESS_CLASSW, nullptr, WS_CHILD, 20, 120, 500, 16, hwnd, (HMENU)IDC_PROGRESS, inst, nullptr);
            g_setup.hDetailsBtn = CreateWindowExW(0, L"BUTTON", fromUtf8(std::string(lang("setup_details")) + "  \xE2\x96\xBC").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 20, 150, 110, 22, hwnd, (HMENU)IDC_DETAILS, inst, nullptr);
            g_setup.hDetailsEdit = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"", WS_CHILD | WS_VSCROLL | ES_MULTILINE | ES_READONLY | ES_AUTOVSCROLL, 20, 176, 540, 120, hwnd, nullptr, inst, nullptr);
            g_setup.hInstall = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_install")).c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 160, 180, 100, 32, hwnd, (HMENU)IDC_INSTALL, inst, nullptr);
            g_setup.hNext = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_next")).c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 380, 200, 80, 28, hwnd, (HMENU)IDC_NEXT, inst, nullptr);
            g_setup.hBack = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("setup_back")).c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 280, 200, 80, 28, hwnd, (HMENU)IDC_BACK, inst, nullptr);
            g_setup.hCancelSetup = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("cancel")).c_str(), WS_CHILD | BS_OWNERDRAW, 280, 200, 80, 28, hwnd, (HMENU)IDC_CANCEL_SETUP, inst, nullptr);
            SendMessageW(g_setup.hTitle, WM_SETFONT, (WPARAM)g_setup.titleFont, TRUE);
            SendMessageW(g_setup.hLangLabel, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hLangCombo, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hThemeLabel, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hThemeCombo, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
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
            SendMessageW(g_setup.hAutoStart, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hAutoLaunch, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hDiskInfo, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hWelcome, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hSummary, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(g_setup.hStartMenu, WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(GetDlgItem(hwnd, 3001), WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SendMessageW(GetDlgItem(hwnd, 3002), WM_SETFONT, (WPARAM)g_setup.font, TRUE);
            SetWindowTheme(g_setup.hPath, L"", L"");
            SetWindowTheme(g_setup.hStartMenu, L"", L"");
            SetWindowTheme(g_setup.hDetailsEdit, L"", L"");
            SetWindowTheme(g_setup.hProgress, L"", L"");
            SetWindowTheme(g_setup.hDiskBar, L"", L"");
            SendMessageW(g_setup.hProgress, PBM_SETBKCOLOR, 0, g_setup.theme.panel);
            SendMessageW(g_setup.hProgress, PBM_SETBARCOLOR, 0, g_setup.theme.accent);
            SendMessageW(g_setup.hDiskBar, PBM_SETBKCOLOR, 0, g_setup.theme.panel);
            SendMessageW(g_setup.hDiskBar, PBM_SETBARCOLOR, 0, g_setup.theme.accent);
            SendMessageW(g_setup.hDiskBar, PBM_SETRANGE32, 0, 100);
            updateDiskInfo();
            if (g_setup.autoStartChecked) SendMessageW(g_setup.hAutoStart, BM_SETCHECK, BST_CHECKED, 0);
            SendMessageW(g_setup.hAutoLaunch, BM_SETCHECK, BST_CHECKED, 0);
            applyWindowTheme(hwnd, g_setup.theme);
            showPageImmediate(0);
            ravex::ui::glowCreate(hwnd, &g_setup.glow);
            g_setup.glow.anywhere = true;
            g_setup.glow.parent = hwnd;
            g_setup.glow.enabled = g_setup.theme.glowEnabled;
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
                        float dx=x-80, dy=y-80; float dist=sqrtf(dx*dx+dy*dy); float t=dist/75.0f; if(t>1) t=1; float s=1-t; float a=s*s*s*90;
                        BYTE al=(BYTE)(a>255?255:a); float f=al/255.0f; int o=(y*160+x)*4; px[o+0]= (BYTE)(255*f); px[o+1]=(BYTE)(255*f); px[o+2]=(BYTE)(255*f); px[o+3]=al;
                    }
                    DeleteObject(g_setup.glow.bmp);
                    g_setup.glow.bmp = whiteBmp;
                }
            }
            {
                std::vector<HWND> btns = {g_setup.hNext, g_setup.hBack, g_setup.hInstall, g_setup.hBrowse, g_setup.hDetailsBtn, g_setup.hLangCombo, g_setup.hStartMenu, g_setup.hProgress};
                ravex::ui::glowSetButtons(&g_setup.glow, btns);
            }
            SetTimer(hwnd, 52, 16, nullptr);
            return 0;
        }
        case WM_COMMAND: {
            int id = LOWORD(wParam);
            if (id == IDC_BROWSE) {
                IFileDialog* pfd = nullptr;
                if (SUCCEEDED(CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&pfd)))) {
                    pfd->SetTitle(fromUtf8(lang("setup_select_folder")).c_str());
                    DWORD opts = 0;
                    pfd->GetOptions(&opts);
                    pfd->SetOptions(opts | FOS_PICKFOLDERS | FOS_FORCEFILESYSTEM | FOS_PATHMUSTEXIST);
                    wchar_t curPath[MAX_PATH]; GetWindowTextW(g_setup.hPath, curPath, MAX_PATH);
                    IShellItem* psiStart = nullptr;
                    if (curPath[0]) {
                        if (SUCCEEDED(SHCreateItemFromParsingName(curPath, nullptr, IID_PPV_ARGS(&psiStart)))) {
                            pfd->SetFolder(psiStart);
                            psiStart->Release();
                        }
                    }
                    if (SUCCEEDED(pfd->Show(hwnd))) {
                        IShellItem* psi = nullptr;
                        if (SUCCEEDED(pfd->GetResult(&psi))) {
                            PWSTR pPath = nullptr;
                            if (SUCCEEDED(psi->GetDisplayName(SIGDN_FILESYSPATH, &pPath))) {
                                SetWindowTextW(g_setup.hPath, pPath);
                                CoTaskMemFree(pPath);
                            }
                            psi->Release();
                        }
                    }
                    pfd->Release();
                }
                updateDiskInfo();
                return 0;
            }
            if (id == IDC_SHORTCUT_DESKTOP) { g_setup.desktopChecked = !g_setup.desktopChecked; g_setup.chkTarget[0] = g_setup.desktopChecked; SetTimer(hwnd, TIMER_CHECK_ANIM, 10, nullptr); InvalidateRect(g_setup.hShortcutDesktop, nullptr, TRUE); return 0; }
            if (id == IDC_SHORTCUT_START) { g_setup.startChecked = !g_setup.startChecked; g_setup.chkTarget[1] = g_setup.startChecked; SetTimer(hwnd, TIMER_CHECK_ANIM, 10, nullptr); InvalidateRect(g_setup.hShortcutStart, nullptr, TRUE); return 0; }
            if (id == IDC_AUTOSTART) { g_setup.autoStartChecked = !g_setup.autoStartChecked; g_setup.chkTarget[2] = g_setup.autoStartChecked; SetTimer(hwnd, TIMER_CHECK_ANIM, 10, nullptr); InvalidateRect(g_setup.hAutoStart, nullptr, TRUE); return 0; }
            if (id == IDC_AUTOLAUNCH) { g_setup.autoLaunchChecked = !g_setup.autoLaunchChecked; g_setup.chkTarget[3] = g_setup.autoLaunchChecked; SetTimer(hwnd, TIMER_CHECK_ANIM, 10, nullptr); InvalidateRect(g_setup.hAutoLaunch, nullptr, TRUE); return 0; }
            if (id == IDC_PATH && HIWORD(wParam) == EN_CHANGE) { updateDiskInfo(); return 0; }
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
            if (id == IDC_THEME_SETUP && HIWORD(wParam) == CBN_SELCHANGE) {
                // deferred: apply AFTER the dropdown has closed and finished its
                // own painting - repainting while the list is still open left
                // stale colour patches behind the labels.
                int sel = (int)SendMessageW(g_setup.hThemeCombo, CB_GETCURSEL, 0, 0);
                g_setup.themeIsCustom = (sel == 4);
                updatePaletteVisibility();
                PostMessageW(hwnd, WM_APP + 21, (WPARAM)sel, 0);
                return 0;
            }
            if (id >= IDC_COLOR_BG && id <= IDC_COLOR_BG + 5) {
                // custom palette swatch: pick any colour - stored in the SAME
                // launcher.json fields the launcher itself uses, so the palette
                // is shared with the launcher (and applied after installation).
                int idx = id - IDC_COLOR_BG;
                static COLORREF s_cust[16] = {
                    RGB(0,0,0), RGB(192,192,192), RGB(128,0,0), RGB(0,128,0),
                    RGB(0,0,128), RGB(128,128,0), RGB(128,0,128), RGB(0,128,128),
                    RGB(192,192,192), RGB(128,128,128), RGB(255,0,0), RGB(0,255,0),
                    RGB(0,0,255), RGB(255,255,0), RGB(255,0,255), RGB(0,255,255)};
                CHOOSECOLORW cc{};
                cc.lStructSize = sizeof(cc);
                cc.hwndOwner = hwnd;
                cc.rgbResult = static_cast<COLORREF>(g_setup.palColors[idx]);
                cc.lpCustColors = s_cust;
                cc.Flags = CC_FULLOPEN | CC_RGBINIT;
                if (ChooseColorW(&cc)) {
                    LauncherConfig lc = loadLauncherConfig();
                    switch (idx) {
                        case 0: lc.customBg = cc.rgbResult; break;
                        case 1: lc.customPanel = cc.rgbResult; break;
                        case 2: lc.customText = cc.rgbResult; break;
                        case 3: lc.customAccent = cc.rgbResult; break;
                        case 4: lc.customButton = cc.rgbResult; break;
                        default: lc.customGlow = cc.rgbResult; break;
                    }
                    saveLauncherConfig(lc);
                    applySetupTheme("custom");   // live repaint with the new palette
                    updatePaletteVisibility();
                }
                return 0;
            }
            if (id == IDC_NEXT) {
                if (g_setup.page == 2) {
                    wchar_t pbuf[MAX_PATH]; GetWindowTextW(g_setup.hPath, pbuf, MAX_PATH);
                    std::string verr; if (!validateInstallPath(pbuf, verr)) {
                        MessageBoxW(hwnd, fromUtf8(verr).c_str(), fromUtf8(lang("setup_title")).c_str(), MB_OK | MB_ICONWARNING);
                        return 0;
                    }
                }
                if (g_setup.page < 5) showPage(g_setup.page + 1);
                return 0;
            }
            if (id == IDC_BACK) {
                if (g_setup.page > 0) showPage(g_setup.page - 1);
                return 0;
            }
            if (id == IDC_CANCEL_SETUP) {
                if (g_setup.busy) { g_setup.cancelRequested = true; g_setup.busy = false; }
                showPage(5);
                return 0;
            }
            if (id == IDC_INSTALL && !g_setup.busy) {
                wchar_t pbuf[MAX_PATH]; GetWindowTextW(g_setup.hPath, pbuf, MAX_PATH);
                std::string verr; if (!validateInstallPath(pbuf, verr)) {
                    MessageBoxW(hwnd, fromUtf8(verr).c_str(), fromUtf8(lang("setup_title")).c_str(), MB_OK | MB_ICONWARNING);
                    return 0;
                }
                g_setup.cancelRequested = false;
                showPage(6);
                std::thread(workerSetup).detach();
                return 0;
            }
            return 0;
        }
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds = (DRAWITEMSTRUCT*)lParam;
            if (ds->hwndItem == g_setup.hWinIcon) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                int bw = rc.right-rc.left, bh = rc.bottom-rc.top; if (bw<=0||bh<=0) return 1;
                HDC memDC = CreateCompatibleDC(dc); HBITMAP memBmp = CreateCompatibleBitmap(dc, bw, bh);
                if (!memDC || !memBmp) { if(memDC) DeleteDC(memDC); if(memBmp) DeleteObject(memBmp); return 1; }
                HGDIOBJ oldBmp = SelectObject(memDC, memBmp);
                RECT mrc{0,0,bw,bh}; HBRUSH bgBr = CreateSolidBrush(g_setup.theme.bg); FillRect(memDC, &mrc, bgBr); DeleteObject(bgBr);
                if (g_setup.winIconBmp && g_setup.winIconBmp->GetLastStatus() == Gdiplus::Ok) {
                    int base = 28;
                    float sc = g_setup.winIconScale;
                    int sw = int(base*sc), sh = int(base*sc);
                    int dx = (bw - sw)/2, dy = (bh - sh)/2;
                    Gdiplus::Graphics g(memDC); g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality); g.SetPixelOffsetMode(Gdiplus::PixelOffsetModeHighQuality);
                    g.DrawImage(g_setup.winIconBmp, Gdiplus::Rect(dx, dy, sw, sh));
                }
                BitBlt(dc, rc.left, rc.top, bw, bh, memDC, 0,0, SRCCOPY);
                SelectObject(memDC, oldBmp); DeleteObject(memBmp); DeleteDC(memDC);
                return 1;
            }
            if (ds->CtlID == IDC_LANG) {
                // language row with flag - same owner-draw style as Settings.
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool sel = (ds->itemState & ODS_SELECTED) != 0;
                COLORREF bg = sel ? g_setup.theme.accent : g_setup.theme.bg;
                COLORREF fg = sel ? RGB(255,255,255) : g_setup.theme.text;
                HBRUSH b = CreateSolidBrush(bg); FillRect(dc, &rc, b); DeleteObject(b);
                if (ds->itemID == (UINT)-1) return TRUE;
                int langIdx = (int)ds->itemID;
                int fx = rc.left + 6;
                HBITMAP hb = (langIdx >= 0 && langIdx < (int)g_setup.flagBmps.size()) ? g_setup.flagBmps[langIdx] : nullptr;
                if (hb) {
                    HDC mdc = CreateCompatibleDC(dc);
                    HGDIOBJ old = SelectObject(mdc, hb);
                    BitBlt(dc, fx, rc.top + (rc.bottom - rc.top - 16)/2, 24, 16, mdc, 0, 0, SRCCOPY);
                    SelectObject(mdc, old); DeleteDC(mdc);
                    fx += 30;
                }
                wchar_t txt[128] = {};
                SendMessageW(g_setup.hLangCombo, CB_GETLBTEXT, ds->itemID, (LPARAM)txt);
                SetBkMode(dc, TRANSPARENT); SetTextColor(dc, fg);
                HFONT oldF = (HFONT)SelectObject(dc, g_setup.font);
                RECT tr = {fx, rc.top, rc.right - 4, rc.bottom};
                DrawTextW(dc, txt, -1, &tr, DT_LEFT | DT_VCENTER | DT_SINGLELINE);
                SelectObject(dc, oldF);
                return TRUE;
            }
            if (ds->CtlID == IDC_SHORTCUT_DESKTOP || ds->CtlID == IDC_SHORTCUT_START || ds->CtlID == IDC_AUTOSTART || ds->CtlID == IDC_AUTOLAUNCH) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool chk = false;
                int animIdx = 0;
                if (ds->CtlID == IDC_SHORTCUT_DESKTOP) { chk = g_setup.desktopChecked; animIdx = 0; }
                else if (ds->CtlID == IDC_SHORTCUT_START) { chk = g_setup.startChecked; animIdx = 1; }
                else if (ds->CtlID == IDC_AUTOSTART) { chk = g_setup.autoStartChecked; animIdx = 2; }
                else { chk = g_setup.autoLaunchChecked; animIdx = 3; }
                int bw = rc.right - rc.left, bh = rc.bottom - rc.top;
                if (bw <= 0 || bh <= 0) return 1;
                // double-buffered: painting straight into the item DC made the
                // label flicker on every animation tick.
                HDC memDC = CreateCompatibleDC(dc);
                HBITMAP memBmp = CreateCompatibleBitmap(dc, bw, bh);
                if (!memDC || !memBmp) { if (memDC) DeleteDC(memDC); if (memBmp) DeleteObject(memBmp); return 1; }
                HGDIOBJ oldBmp = SelectObject(memDC, memBmp);
                RECT mrc = {0, 0, bw, bh};
                HBRUSH bgFill = CreateSolidBrush(g_setup.theme.bg); FillRect(memDC, &mrc, bgFill); DeleteObject(bgFill);
                Gdiplus::Graphics g(memDC);
                g.TranslateTransform((Gdiplus::REAL)-rc.left, (Gdiplus::REAL)-rc.top);
                g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintAntiAliasGridFit);
                int box = 18; int by = rc.top + (rc.bottom - rc.top - box)/2; int bx = rc.left + 4;
                Gdiplus::Color fill = chk ? Gdiplus::Color(255, 0,120,212) : Gdiplus::Color(255,45,45,47);
                Gdiplus::Color border = chk ? Gdiplus::Color(255, 0,120,212) : Gdiplus::Color(255,110,110,115);
                Gdiplus::SolidBrush br(fill);
                Gdiplus::Pen pen(border, 1.5f);
                Gdiplus::GraphicsPath path;
                path.AddArc(bx, by, 3, 3, 180, 90); path.AddArc(bx+box-3, by, 3, 3, 270, 90); path.AddArc(bx+box-3, by+box-3, 3, 3, 0, 90); path.AddArc(bx, by+box-3, 3, 3, 90, 90); path.CloseFigure();
                g.FillPath(&br, &path); g.DrawPath(&pen, &path);
                {
                    // smooth tick: grows/fades in and out with chkAnim[] progress.
                    float a = g_setup.chkAnim[animIdx];
                    if (a > 0.01f) {
                        float ax = (float)(bx+5), ay = (float)(by+9);
                        float mx = ax + ((bx+8) - ax) * a, my = ay + ((by+12) - ay) * a;
                        float ex = ax + ((bx+13) - ax) * a, ey = ay + ((by+6) - ay) * a;
                        Gdiplus::Pen cpen(Gdiplus::Color((BYTE)(255.0f * a), 255,255,255), 1.8f);
                        cpen.SetLineCap(Gdiplus::LineCapRound, Gdiplus::LineCapRound, Gdiplus::DashCapFlat);
                        g.DrawLine(&cpen, ax, ay, mx, my);
                        g.DrawLine(&cpen, mx, my, ex, ey);
                    }
                }
                Gdiplus::SolidBrush tbrush(Gdiplus::Color(255, GetRValue(g_setup.theme.text), GetGValue(g_setup.theme.text), GetBValue(g_setup.theme.text)));
                Gdiplus::Font f(memDC, g_setup.font);
                RECT tr = {bx+box+10, rc.top, rc.right, rc.bottom};
                Gdiplus::RectF rf((Gdiplus::REAL)tr.left, (Gdiplus::REAL)tr.top, (Gdiplus::REAL)(tr.right-tr.left), (Gdiplus::REAL)(tr.bottom-tr.top));
                Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentNear); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                wchar_t txt[128]; GetWindowTextW(ds->hwndItem, txt, 128);
                g.DrawString(txt, -1, &f, rf, &fmt, &tbrush);
                g.Flush();
                // source must be the buffer origin (0,0) - the bitmap is only
                // bw x bh, so taking rc.left/rc.top clipped the copy and left
                // the checkbox rows smudged on every repaint.
                BitBlt(dc, rc.left, rc.top, bw, bh, memDC, 0, 0, SRCCOPY);
                SelectObject(memDC, oldBmp); DeleteObject(memBmp); DeleteDC(memDC);
                return 1;
            }
            if (ds->CtlID == IDC_BROWSE || ds->CtlID == IDC_NEXT || ds->CtlID == IDC_BACK || ds->CtlID == IDC_INSTALL || ds->CtlID == IDC_DETAILS || ds->CtlID == IDC_CANCEL_SETUP
                || (ds->CtlID >= IDC_COLOR_BG && ds->CtlID <= IDC_COLOR_BG + 5)) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool isPressed = (ds->itemState & ODS_SELECTED) != 0;
                bool isDisabled = (ds->itemState & ODS_DISABLED) != 0;
                POINT cur; GetCursorPos(&cur); RECT wr; GetWindowRect(ds->hwndItem, &wr);
                bool isHover = PtInRect(&wr, cur);
                bool isDark = g_setup.theme.isDark;
                COLORREF bg = isDark ? RGB(0,0,0) : RGB(248,248,250);
                COLORREF fg = isDark ? RGB(240,240,240) : RGB(18,18,20);
                if (isDisabled) { bg = isDark ? RGB(20,20,20) : RGB(220,220,220); fg = isDark ? RGB(100,100,100) : RGB(150,150,150); }
                else if (isPressed) { bg = isDark ? RGB(18,18,18) : RGB(220,220,230); }
                else if (isHover) { bg = isDark ? RGB(18,18,18) : RGB(235,235,238); fg = isDark ? RGB(255,255,255) : RGB(18,18,20); }
                bool isColorBtn = (ds->CtlID >= IDC_COLOR_BG && ds->CtlID <= IDC_COLOR_BG + 5);
                if (isColorBtn && !isDisabled) {
                    // swatch shows the actual palette colour, text picks its contrast.
                    bg = static_cast<COLORREF>(g_setup.palColors[ds->CtlID - IDC_COLOR_BG]);
                    int lr = GetRValue(bg), lg = GetGValue(bg), lb = GetBValue(bg);
                    fg = (0.299 * lr + 0.587 * lg + 0.114 * lb) > 140 ? RGB(20,20,20) : RGB(245,245,245);
                }
                int bw = rc.right - rc.left, bh = rc.bottom - rc.top; if (bw <=0 || bh <=0) return 1;
                RECT prc = rc; if (isPressed) { prc.left+=2; prc.top+=1; prc.right-=2; prc.bottom-=1; }
                HDC memDC = CreateCompatibleDC(dc); HBITMAP memBmp = CreateCompatibleBitmap(dc, bw, bh);
                if (!memDC || !memBmp) { if(memDC) DeleteDC(memDC); if(memBmp) DeleteObject(memBmp); return 1; }
                HGDIOBJ oldBmp = SelectObject(memDC, memBmp);
                RECT mrc{0,0,bw,bh}; HBRUSH bgBr = CreateSolidBrush(g_setup.theme.bg); FillRect(memDC, &mrc, bgBr); DeleteObject(bgBr);
                Gdiplus::Graphics g(memDC); g.TranslateTransform((Gdiplus::REAL)-rc.left, (Gdiplus::REAL)-rc.top);
                Gdiplus::GraphicsPath path; path.AddArc(prc.left,prc.top,8,8,180,90); path.AddArc(prc.right-8,prc.top,8,8,270,90); path.AddArc(prc.right-8,prc.bottom-8,8,8,0,90); path.AddArc(prc.left,prc.bottom-8,8,8,90,90); path.CloseFigure();
                Gdiplus::SolidBrush br(Gdiplus::Color(GetRValue(bg), GetGValue(bg), GetBValue(bg))); g.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias); g.FillPath(&br, &path);
                if (g_setup.theme.glowEnabled && !isColorBtn) {
                    float fx=float(cur.x-wr.left), fy=float(cur.y-wr.top), rbw=float(wr.right-wr.left), rbh=float(wr.bottom-wr.top);
                    float nx=(fx<0)?0:((fx>rbw)?rbw:fx), ny=(fy<0)?0:((fy>rbh)?rbh:fy);
                    float dx=fx-nx, dy=fy-ny, d=sqrtf(dx*dx+dy*dy);
                    BYTE glowR=GetRValue(g_setup.theme.glow), glowG=GetGValue(g_setup.theme.glow), glowB=GetBValue(g_setup.theme.glow);
                    float cfx=(fx<1)?1:((fx>rbw-1)?rbw-1:fx), cfy=(fy<1)?1:((fy>rbh-1)?rbh-1:fy);
                    if (isPressed) { d=0; cfx=rbw*0.5f; cfy=rbh*0.5f; }
                    const float innerRadius=280.0f;
                    if (d < innerRadius) { float it=1-d/innerRadius; it*=it; BYTE bgA=BYTE(it*(isPressed?32:(isHover?95:45))); if(bgA>0){ Gdiplus::PathGradientBrush bgGrad(&path); bgGrad.SetCenterPoint(Gdiplus::PointF(cfx,cfy)); bgGrad.SetCenterColor(Gdiplus::Color(bgA,glowR,glowG,glowB)); int cnt2=1; Gdiplus::Color sur2(0,glowR,glowG,glowB); bgGrad.SetSurroundColors(&sur2,&cnt2); bgGrad.SetFocusScales(0.18f,0.18f); g.FillPath(&bgGrad,&path); } }
                    if (d < 160) { float t=1-d/160; float bt=t*t; BYTE a=BYTE(40+bt*(isHover?190:120)); for(int pass=0;pass<2;++pass){ Gdiplus::GraphicsPath border; float off=(pass==0)?1.0f:0.0f; float r=(pass==0)?6.0f:7.0f; border.AddArc((Gdiplus::REAL)prc.left+off,(Gdiplus::REAL)prc.top+off,r,r,180,90); border.AddArc((Gdiplus::REAL)prc.right-off-r,(Gdiplus::REAL)prc.top+off,r,r,270,90); border.AddArc((Gdiplus::REAL)prc.right-off-r,(Gdiplus::REAL)prc.bottom-off-r,r,r,0,90); border.AddArc((Gdiplus::REAL)prc.left+off,(Gdiplus::REAL)prc.bottom-off-r,r,r,90,90); border.CloseFigure(); Gdiplus::PathGradientBrush grad(&border); grad.SetCenterPoint(Gdiplus::PointF(cfx,cfy)); grad.SetCenterColor(Gdiplus::Color(pass==0?BYTE(a*3/4):a,glowR,glowG,glowB)); int cnt=1; Gdiplus::Color sur(0,glowR,glowG,glowB); grad.SetSurroundColors(&sur,&cnt); grad.SetFocusScales(0.3f,0.3f); float pw=(pass==0)?2.0f:(isHover?1.4f:0.8f); Gdiplus::Pen pen(&grad,pw); pen.SetLineJoin(Gdiplus::LineJoinRound); g.DrawPath(&pen,&border); } }
                }
                wchar_t txt[128]; GetWindowTextW(ds->hwndItem, txt, 128);
                Gdiplus::SolidBrush tbr(Gdiplus::Color(GetRValue(fg),GetGValue(fg),GetBValue(fg)));
                Gdiplus::Font f(memDC, g_setup.font);
                Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentCenter); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                Gdiplus::RectF rf((Gdiplus::REAL)prc.left,(Gdiplus::REAL)prc.top,(Gdiplus::REAL)(prc.right-prc.left),(Gdiplus::REAL)(prc.bottom-prc.top));
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit); g.SetCompositingQuality(Gdiplus::CompositingQualityHighQuality); g.DrawString(txt,-1,&f,rf,&fmt,&tbr);
                BitBlt(dc, rc.left, rc.top, bw, bh, memDC, 0,0, SRCCOPY);
                SelectObject(memDC, oldBmp); DeleteObject(memBmp); DeleteDC(memDC);
                return 1;
            }
            return 0;
        }
        case WM_CTLCOLORSTATIC: {
            HDC dc = (HDC)wParam;
            SetTextColor(dc, g_setup.theme.text);
            SetBkColor(dc, g_setup.theme.bg);
            // OPAQUE (same as the main launcher): the static repaints its own
            // rectangle with the CURRENT theme brush on every paint. The old
            // TRANSPARENT mode never cleared the surface, so bars of the
            // previously selected theme stayed behind the labels.
            SetBkMode(dc, OPAQUE);
            return (LRESULT)g_setup.bgBrush;
        }
        case WM_CTLCOLORBTN: {
            HDC dc = (HDC)wParam;
            SetTextColor(dc, g_setup.theme.text);
            SetBkColor(dc, g_setup.theme.bg);
            SetBkMode(dc, OPAQUE);
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
                g_setup.spinnerPhase += 0.024f;
                if (g_setup.spinnerPhase > kWin10Cycle) g_setup.spinnerPhase -= kWin10Cycle;
                RECT rc; GetClientRect(hwnd, &rc);
                RECT sr = { (rc.right-60)/2, 120, (rc.right+60)/2, 180 };
                InvalidateRect(hwnd, &sr, FALSE);
                return 0;
            }
            if (wParam == 52) { ravex::ui::glowUpdateAnywhere(hwnd, &g_setup.glow); for (HWND b : g_setup.glow.buttons) if(b) InvalidateRect(b, nullptr, FALSE); if (fabs(g_setup.winIconScale - g_setup.winIconTarget) > 0.01f) { g_setup.winIconScale += (g_setup.winIconTarget - g_setup.winIconScale) * 0.18f; InvalidateRect(g_setup.hWinIcon, nullptr, FALSE); } else if (g_setup.winIconScale != g_setup.winIconTarget) { g_setup.winIconScale = g_setup.winIconTarget; InvalidateRect(g_setup.hWinIcon, nullptr, FALSE); } return 0; }
            if (wParam == TIMER_CHECK_ANIM && g_setup.hwnd) {
                const float step = 10.0f / static_cast<float>(CHECK_ANIM_MS);
                bool moving = false;
                HWND ctls[4] = {g_setup.hShortcutDesktop, g_setup.hShortcutStart, g_setup.hAutoStart, g_setup.hAutoLaunch};
                for (int i = 0; i < 4; ++i) {
                    float& a = g_setup.chkAnim[i];
                    float before = a;
                    float tgt = g_setup.chkTarget[i] ? 1.0f : 0.0f;
                    if (a < tgt) { a += step; if (a > tgt) a = tgt; }
                    else if (a > tgt) { a -= step; if (a < tgt) a = tgt; }
                    if (a != before) {
                        moving = true;
                        // repaint ONLY the checkbox that actually changed - redrawing
                        // all four every tick made their labels flicker.
                        if (ctls[i] && IsWindow(ctls[i])) InvalidateRect(ctls[i], nullptr, FALSE);
                    }
                }
                if (!moving) KillTimer(hwnd, TIMER_CHECK_ANIM);
                return 0;
            }
            if (wParam == TIMER_PAGE_ANIM && g_setup.animActive) {
                g_setup.animT += 10.0f / static_cast<float>(PAGE_ANIM_MS);
                float t = g_setup.animT;
                if (t > 1.0f) t = 1.0f;
                float e = 1.0f - (1.0f - t) * (1.0f - t) * (1.0f - t);  // easeOutCubic
                int outOff = static_cast<int>(PAGE_SLIDE * e);
                int inOff = static_cast<int>(PAGE_SLIDE * (e - 1.0f));
                for (size_t i = 0; i < g_setup.animOut.size(); ++i) {
                    HWND h = g_setup.animOut[i];
                    if (!IsWindow(h)) continue;
                    const POINT& p0 = g_setup.animOutP0[i];
                    SetWindowPos(h, nullptr, p0.x, p0.y + outOff, 0, 0, SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
                    SetLayeredWindowAttributes(h, 0, static_cast<BYTE>(255 * (1.0f - t)), LWA_ALPHA);
                }
                for (size_t i = 0; i < g_setup.animIn.size(); ++i) {
                    HWND h = g_setup.animIn[i];
                    if (!IsWindow(h)) continue;
                    const POINT& p0 = g_setup.animInP0[i];
                    SetWindowPos(h, nullptr, p0.x, p0.y + inOff, 0, 0, SWP_NOSIZE | SWP_NOZORDER | SWP_NOACTIVATE);
                    SetLayeredWindowAttributes(h, 0, static_cast<BYTE>(255 * t), LWA_ALPHA);
                }
                if (t >= 1.0f) finishPageAnim();
                return 0;
            }
            if (wParam == 99) {
                KillTimer(hwnd, 99);
                if (g_setup.taskbar && g_setup.taskbarReady) {
                    g_setup.taskbar->SetProgressState(hwnd, TBPF_NOPROGRESS);
                }
                return 0;
            }
            return 0;
        }
        case WM_PAINT: {
            PAINTSTRUCT ps; HDC hdc = BeginPaint(hwnd, &ps);
            // live theme colour: a cached brush could still carry the previous
            // theme's background (left-over teal bars after switching back).
            HBRUSH liveBg = CreateSolidBrush(g_setup.theme.bg);
            FillRect(hdc, &ps.rcPaint, liveBg);
            DeleteObject(liveBg);
            RECT rc; GetClientRect(hwnd, &rc);
            int w = rc.right - rc.left;
            if (g_setup.page == 6) {
                int cx = w/2; int cy = 150;
                drawSpinner(hdc, cx, cy, g_setup.spinnerPhase);
            }
            if (g_setup.page < 6) {
                int steps = 6;
                int dotR = 10;
                int gap = 56;
                int totalW = (steps - 1) * gap;
                int startX = (w - totalW) / 2;
                int dotY = 52;
                // render the whole step band off-screen. Drawing antialiased GDI+
                // strokes directly inside a partial update region clips the
                // blending samples, which left the white ring around the current
                // step visibly jagged; in a memory DC nothing is clipped.
                HDC hdrMem = CreateCompatibleDC(hdc);
                HBITMAP hdrBmp = (w > 0) ? CreateCompatibleBitmap(hdc, w, 90) : nullptr;
                HGDIOBJ hdrOld = nullptr;
                HDC hdrDC = hdc;
                if (hdrMem && hdrBmp) {
                    hdrOld = SelectObject(hdrMem, hdrBmp);
                    RECT band = {0, 0, w, 90};
                    FillRect(hdrMem, &band, g_setup.bgBrush);
                    hdrDC = hdrMem;
                } else {
                    if (hdrBmp) DeleteObject(hdrBmp);
                    if (hdrMem) DeleteDC(hdrMem);
                    hdrMem = nullptr;
                    hdrBmp = nullptr;
                }
                Gdiplus::Graphics g(hdrDC);
                g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                // grayscale AA renders much cleaner than ClearType on dark panels.
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintAntiAliasGridFit);
                g.SetCompositingQuality(Gdiplus::CompositingQualityHighQuality);
                g.SetPixelOffsetMode(Gdiplus::PixelOffsetModeHighQuality);
                g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
                Gdiplus::Font numFont(L"Segoe UI", 9.0f, Gdiplus::FontStyleBold, Gdiplus::UnitPoint);
                Gdiplus::Font labelFont(L"Segoe UI", 9.0f, Gdiplus::FontStyleRegular, Gdiplus::UnitPoint);
                for (int i = 0; i < steps; ++i) {
                    int cx = startX + i * gap;
                    bool completed = (i < g_setup.page);
                    bool current = (i == g_setup.page);
                    if (i < steps - 1) {
                        Gdiplus::Color lineCol;
                        if (i < g_setup.page) lineCol = Gdiplus::Color(220, GetRValue(g_setup.theme.accent), GetGValue(g_setup.theme.accent), GetBValue(g_setup.theme.accent));
                        else lineCol = Gdiplus::Color(80, GetRValue(g_setup.theme.text), GetGValue(g_setup.theme.text), GetBValue(g_setup.theme.text));
                        Gdiplus::Pen linePen(lineCol, 2.0f);
                        g.DrawLine(&linePen, (float)(cx + dotR + 3), (float)dotY, (float)(cx + gap - dotR - 3), (float)dotY);
                    }
                    if (current) {
                        Gdiplus::Color glowCol(50, GetRValue(g_setup.theme.accent), GetGValue(g_setup.theme.accent), GetBValue(g_setup.theme.accent));
                        Gdiplus::SolidBrush glowBr(glowCol);
                        g.FillEllipse(&glowBr, cx - dotR - 5, dotY - dotR - 5, (dotR + 5) * 2, (dotR + 5) * 2);
                    }
                    Gdiplus::Color fillCol;
                    if (completed || current) {
                        fillCol = Gdiplus::Color(255, GetRValue(g_setup.theme.accent), GetGValue(g_setup.theme.accent), GetBValue(g_setup.theme.accent));
                    } else {
                        fillCol = Gdiplus::Color(255, GetRValue(g_setup.theme.panel), GetGValue(g_setup.theme.panel), GetBValue(g_setup.theme.panel));
                    }
                    Gdiplus::SolidBrush br(fillCol);
                    g.FillEllipse(&br, cx - dotR, dotY - dotR, dotR * 2, dotR * 2);
                    if (current) {
                        Gdiplus::Pen pen(Gdiplus::Color(255, GetRValue(g_setup.theme.text), GetGValue(g_setup.theme.text), GetBValue(g_setup.theme.text)), 2.0f);
                        g.DrawEllipse(&pen, cx - dotR - 2, dotY - dotR - 2, dotR * 2 + 4, dotR * 2 + 4);
                    }
                    if (completed) {
                        Gdiplus::Pen cpen(Gdiplus::Color(255, 255, 255, 255), 2.0f);
                        cpen.SetLineCap(Gdiplus::LineCapRound, Gdiplus::LineCapRound, Gdiplus::DashCapFlat);
                        g.DrawLine(&cpen, (float)(cx - 4), (float)dotY, (float)(cx - 1), (float)(dotY + 3));
                        g.DrawLine(&cpen, (float)(cx - 1), (float)(dotY + 3), (float)(cx + 5), (float)(dotY - 4));
                    } else {
                        wchar_t num[4]; _swprintf(num, L"%d", i + 1);
                        Gdiplus::SolidBrush textBr(Gdiplus::Color(255, GetRValue(g_setup.theme.text), GetGValue(g_setup.theme.text), GetBValue(g_setup.theme.text)));
                        Gdiplus::RectF layoutRect((float)(cx - dotR), (float)(dotY - dotR), (float)(dotR * 2), (float)(dotR * 2));
                        Gdiplus::StringFormat sf; sf.SetAlignment(Gdiplus::StringAlignmentCenter); sf.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                        g.DrawString(num, -1, &numFont, layoutRect, &sf, &textBr);
                    }
                    const char* stepKeys[] = {"setup_step_lang","setup_step_welcome","setup_step_folder","setup_step_menu","setup_step_ready","setup_step_install"};
                    const char* stepFallback[] = {"Language","Welcome","Folder","Menu","Ready","Install"};
                    const char* sk = stepKeys[i];
                    const char* sv = ravex::lang(sk);
                    if (!sv || strcmp(sv, sk) == 0) sv = stepFallback[i];
                    wchar_t wlabel[64];
                    MultiByteToWideChar(CP_UTF8, 0, sv, -1, wlabel, 64);
                    Gdiplus::Color labelCol = current || completed
                        ? Gdiplus::Color(255, GetRValue(g_setup.theme.text), GetGValue(g_setup.theme.text), GetBValue(g_setup.theme.text))
                        : Gdiplus::Color(140, GetRValue(g_setup.theme.text), GetGValue(g_setup.theme.text), GetBValue(g_setup.theme.text));
                    Gdiplus::SolidBrush labelBr(labelCol);
                    Gdiplus::RectF labelRect((float)(cx - 35), (float)(dotY + dotR + 5), 70.0f, 20.0f);
                    Gdiplus::StringFormat lsf; lsf.SetAlignment(Gdiplus::StringAlignmentCenter);
                    g.DrawString(wlabel, -1, &labelFont, labelRect, &lsf, &labelBr);
                }
                if (hdrMem && hdrBmp) {
                    g.Flush();  // make sure GDI+ finished drawing into the buffer
                    BitBlt(hdc, 0, 0, w, 90, hdrMem, 0, 0, SRCCOPY);
                    SelectObject(hdrMem, hdrOld);
                    DeleteObject(hdrBmp);
                    DeleteDC(hdrMem);
                }
            }
            EndPaint(hwnd, &ps);
            return 0;
        }
        case WM_MOUSEMOVE: { ravex::ui::glowUpdateAnywhere(hwnd, &g_setup.glow); for (HWND b : g_setup.glow.buttons) if (b) InvalidateRect(b, nullptr, FALSE); POINT curPt; GetCursorPos(&curPt); RECT wR; GetWindowRect(g_setup.hWinIcon, &wR); bool wHov = PtInRect(&wR, curPt); if (wHov != g_setup.winIconHovered) { g_setup.winIconHovered = wHov; g_setup.winIconTarget = wHov ? 1.22f : 1.0f; } return 0; }
        case WM_APP + 21: {
            static const char* kThemeNamesByIndex[5] = {"dark", "light", "win95", "vista", "custom"};
            int sel = (int)wParam;
            if (sel >= 0 && sel < 5) applySetupTheme(kThemeNamesByIndex[sel]);
            return 0;
        }
        case WM_APP + 10: {
            std::string* p = (std::string*)lParam;
            setStatus(*p); appendDetail(*p); delete p;
            if (g_setup.taskbar && g_setup.taskbarReady) {
                g_setup.taskbar->SetProgressState(hwnd, TBPF_INDETERMINATE);
            }
            return 0;
        }
        case WM_APP + 11: {
            std::wstring* p = (std::wstring*)lParam;
            KillTimer(hwnd, 50);
            g_setup.busy = false;
            if (g_setup.taskbar && g_setup.taskbarReady) {
                g_setup.taskbar->SetProgressState(hwnd, TBPF_PAUSED);
                SetTimer(hwnd, 99, 5000, nullptr);
            }
            { std::wstring msg = fromUtf8(lang("setup_installed_msg")); size_t pos = msg.find(L"{path}"); if(pos != std::wstring::npos) msg.replace(pos, 6, *p);
            MessageBoxW(hwnd, msg.c_str(), fromUtf8(lang("setup_title")).c_str(), MB_OK | MB_ICONINFORMATION); }
            delete p;
            DestroyWindow(hwnd);
            return 0;
        }
        case WM_APP + 12: {
            g_setup.detailsVisible = true;
            ShowWindow(g_setup.hDetailsEdit, SW_SHOW);
            RECT rc; GetClientRect(hwnd, &rc);
            SendMessageW(hwnd, WM_SIZE, 0, MAKELPARAM(rc.right, rc.bottom));
            InvalidateRect(hwnd, nullptr, TRUE);
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
            HDWP dwp = BeginDeferWindowPos(32);
            if (dwp) {
                auto placeFixed = [&](HWND hw, int x, int y, int cw, int ch){ DeferWindowPos(dwp, hw, nullptr, x, y, cw, ch, SWP_NOZORDER); };
                auto placeCard = [&](HWND hw, int x, int y, int cw, int ch){ DeferWindowPos(dwp, hw, nullptr, x, y+40, cw, ch, SWP_NOZORDER); };
                placeFixed(g_setup.hTitle, 0, 10, w, 26);
                placeFixed(g_setup.hWinIcon, 12, h - 52, 44, 44);
                placeFixed(g_setup.hNext, w - 100, h - 50, 80, 28);
                placeFixed(g_setup.hBack, w - 190, h - 50, 80, 28);
                placeFixed(g_setup.hInstall, w/2 - 50, h - 50, 100, 32);
                placeFixed(g_setup.hCancelSetup, w/2 - 50, h - 50, 100, 32);
                placeCard(g_setup.hLangCombo, 20, 74, 300, 200);
                placeCard(g_setup.hLangLabel, 20, 50, w-40, 22);
                placeCard(g_setup.hThemeCombo, 20, 132, 300, 200);
                placeCard(g_setup.hThemeLabel, 20, 108, w-40, 22);
                placeCard(g_setup.hPaletteLabel, 20, 170, 400, 18);
                placeCard(g_setup.hColorBtns[0], 20, 196, 120, 26);
                placeCard(g_setup.hColorBtns[1], 150, 196, 120, 26);
                placeCard(g_setup.hColorBtns[2], 280, 196, 120, 26);
                placeCard(g_setup.hColorBtns[3], 20, 228, 120, 26);
                placeCard(g_setup.hColorBtns[4], 150, 228, 120, 26);
                placeCard(g_setup.hColorBtns[5], 280, 228, 120, 26);
                placeCard(g_setup.hDiskIcon, 20, 78, 24, 24);
                placeCard(g_setup.hPath, 50, 80, w - 170, 24);
                placeCard(g_setup.hBrowse, w - 110, 80, 90, 24);
                placeCard(g_setup.hDiskInfo, 20, 110, 300, 16);
                placeCard(g_setup.hDiskBar, 330, 110, 160, 14);
                placeCard(g_setup.hStartMenu, 20, 80, w-40, 24);
                placeCard(g_setup.hShortcutDesktop, 20, 65, 260, 28);
                placeCard(g_setup.hShortcutStart, 20, 100, 280, 28);
                placeCard(g_setup.hAutoStart, 20, 135, 280, 28);
                placeCard(g_setup.hAutoLaunch, 20, 170, 280, 28);
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
            HDC dc = (HDC)wParam; RECT rc; GetClientRect(hwnd, &rc);
            HBRUSH liveBg = CreateSolidBrush(g_setup.theme.bg);
            FillRect(dc, &rc, liveBg);
            DeleteObject(liveBg);
            return 1;
        }
        case WM_DESTROY: {
            KillTimer(hwnd, 50); KillTimer(hwnd, 51); KillTimer(hwnd, 52); KillTimer(hwnd, 99); KillTimer(hwnd, TIMER_PAGE_ANIM); KillTimer(hwnd, TIMER_CHECK_ANIM);
            for (HBITMAP hb : g_setup.flagBmps) if (hb) DeleteObject(hb);
            g_setup.flagBmps.clear();
            if (g_setup.hDiskIco) { DestroyIcon(g_setup.hDiskIco); g_setup.hDiskIco = nullptr; }
            if (g_setup.taskbar) { g_setup.taskbar->Release(); g_setup.taskbar = nullptr; }
            ravex::ui::glowDestroy(&g_setup.glow);
            if (g_setup.font) DeleteObject(g_setup.font);
            if (g_setup.titleFont) DeleteObject(g_setup.titleFont);
            if (g_setup.bgBrush) DeleteObject(g_setup.bgBrush);
            if (g_setup.winIconBmp) delete g_setup.winIconBmp;
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
            std::wstring kickxVer = fromUtf8(RAVEX_VERSION);
            RegSetValueExW(hk,L"DisplayVersion",0,REG_SZ,(BYTE*)kickxVer.c_str(), (DWORD)((kickxVer.size()+1)*sizeof(wchar_t)));
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
void deleteOwnEntry(const std::wstring& path) {
    DWORD attr = GetFileAttributesW(path.c_str());
    if (attr == INVALID_FILE_ATTRIBUTES) return;
    if (attr & FILE_ATTRIBUTE_DIRECTORY) {
        SHFILEOPSTRUCTW op{};
        op.wFunc = FO_DELETE;
        std::wstring from = path + L'\0';
        from.push_back(L'\0');
        op.pFrom = from.c_str();
        op.fFlags = FOF_NOCONFIRMATION | FOF_NOERRORUI | FOF_SILENT;
        SHFileOperationW(&op);
    } else {
        SetFileAttributesW(path.c_str(), FILE_ATTRIBUTE_NORMAL);
        DeleteFileW(path.c_str());
    }
}
bool performUninstall(const std::wstring& installDir) {
    std::wstring dir = installDir;
    if (dir.empty()) { PWSTR p=nullptr; if(SHGetKnownFolderPath(FOLDERID_LocalAppData,0,nullptr,&p)==S_OK){ dir=std::wstring(p)+L"\\KickX"; CoTaskMemFree(p);} else dir=kickxDir(); }
    PWSTR pDesk=nullptr; if(SHGetKnownFolderPath(FOLDERID_Desktop,0,nullptr,&pDesk)==S_OK){ DeleteFileW((std::wstring(pDesk)+L"\\KickX.lnk").c_str()); CoTaskMemFree(pDesk); }
    PWSTR pStart=nullptr; if(SHGetKnownFolderPath(FOLDERID_StartMenu,0,nullptr,&pStart)==S_OK){ DeleteFileW((std::wstring(pStart)+L"\\KickX\\KickX.lnk").c_str()); RemoveDirectoryW((std::wstring(pStart)+L"\\KickX").c_str()); CoTaskMemFree(pStart); }
    PWSTR pStartup=nullptr; if(SHGetKnownFolderPath(FOLDERID_Startup,0,nullptr,&pStartup)==S_OK){ DeleteFileW((std::wstring(pStartup)+L"\\KickX.lnk").c_str()); CoTaskMemFree(pStartup); }
    while (!dir.empty() && (dir.back() == L'\\' || dir.back() == L'/')) dir.pop_back();
    bool driveRoot = dir.size() >= 2 && dir.size() <= 3 && dir[1] == L':';
    if (!dir.empty() && !driveRoot && fileExists(dir)) {
        std::wstring manifestPath = joinPath(dir, L"kickx_manifest.txt");
        std::string manifestText;
        if (readFile(manifestPath, manifestText)) {
            std::size_t pos = 0;
            while (pos < manifestText.size()) {
                std::size_t nl = manifestText.find('\n', pos);
                std::string entry = (nl == std::string::npos) ? manifestText.substr(pos) : manifestText.substr(pos, nl - pos);
                pos = (nl == std::string::npos) ? manifestText.size() : nl + 1;
                if (!entry.empty() && entry.back() == '\r') entry.pop_back();
                if (entry.empty()) continue;
                bool plain = entry.find('\\') == std::string::npos && entry.find('/') == std::string::npos && entry != "." && entry != "..";
                if (!plain) continue;
                deleteOwnEntry(joinPath(dir, fromUtf8(entry)));
            }
        } else if (fileExists(joinPath(dir, L"kickx_launcher.exe")) || fileExists(joinPath(dir, L"KickXSetup.exe"))) {
            deleteOwnEntry(joinPath(dir, L"kickx_launcher.exe"));
            deleteOwnEntry(joinPath(dir, L"KickXSetup.exe"));
            deleteOwnEntry(joinPath(dir, L"icons"));
            deleteOwnEntry(joinPath(dir, L"flags"));
            deleteOwnEntry(joinPath(dir, L"README.md"));
        }
        DeleteFileW(manifestPath.c_str());
        RemoveDirectoryW(dir.c_str());
    }
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
    initWin10LUT();
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
    UINT tbMsg = RegisterWindowMessageW(L"TaskbarButtonCreated");
    ChangeWindowMessageFilterEx(hwnd, tbMsg, MSGFLT_ALLOW, nullptr);
    g_setup.taskbarReady = false;
    ShowWindow(hwnd, SW_SHOW); UpdateWindow(hwnd);
    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0)) { TranslateMessage(&msg); DispatchMessageW(&msg); }
    Gdiplus::GdiplusShutdown(tok);
    return true;
}
}
