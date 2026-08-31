#include "ui/include/splash_window.hpp"
#include "core/include/config.hpp"
#include "core/include/lang.hpp"
#include "core/include/paths.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "game/include/ravex.hpp"
#include <windows.h>
#include <dwmapi.h>
#include <commctrl.h>
#include <gdiplus.h>
#include <cmath>
#include <cstdlib>
#include <string>
#include <thread>
#include <vector>
namespace ravex::ui {
namespace {
constexpr UINT WM_SPLASH_STATUS = WM_APP + 100;
constexpr UINT WM_SPLASH_DONE = WM_APP + 102;
constexpr UINT_PTR TIMER_SPINNER = 1;
constexpr UINT_PTR TIMER_PHRASE = 2;
constexpr UINT SPINNER_INTERVAL_MS = 16;
constexpr UINT PHRASE_INTERVAL_MS = 2200;
constexpr int SPINNER_RADIUS = 18;
constexpr int SPINNER_CX = 260;
constexpr int SPINNER_CY = 92;
constexpr int SPINNER_DOTS = 5;
constexpr float SPINNER_DOT_DELAY = 0.24f;
constexpr float SPINNER_DOT_RADIUS = 2.5f;
constexpr float SPINNER_FADE_IN = 0.05f;
constexpr float SPINNER_FADE_OUT = 0.03f;
constexpr float SPINNER_PI = 3.14159265f;
constexpr int SPINNER_LUT_SIZE = 256;
const char* kPhraseKeys[] = {
    "splash_preparing",
    "splash_loading",
    "splash_ready",
};
constexpr int kPhraseCount = 3;
struct SplashData {
    HWND hwnd = nullptr;
    HWND hStatus = nullptr;
    HWND hTitle = nullptr;
    HFONT font = nullptr;
    HFONT titleFont = nullptr;
    HBRUSH bgBrush = nullptr;
    ThemeColors theme;
    bool done = false;
    float animPhase = 0.0f;
    float lut[SPINNER_LUT_SIZE] = {};
    int phraseIndex = 0;
    Gdiplus::Bitmap* winLogo = nullptr;
};
SplashData g_splash;
std::wstring iconPathSplash(const std::wstring& name) {
    wchar_t buf[MAX_PATH]; GetModuleFileNameW(nullptr, buf, MAX_PATH);
    std::wstring s = buf; size_t p = s.find_last_of(L"\\/"); if (p != std::wstring::npos) s = s.substr(0, p + 1);
    std::wstring cand = s + L"icons\\" + name + L".png";
    if (fileExists(cand)) return cand;
    std::wstring cur = s;
    for (int i = 0; i < 5; ++i) {
        size_t pos = cur.find_last_of(L"\\/");
        if (pos == std::wstring::npos) break;
        cur = cur.substr(0, pos);
        if (cur.empty()) break;
        cand = cur + L"\\icons\\" + name + L".png";
        if (fileExists(cand)) return cand;
        cand = cur + L"\\src\\main\\cpp\\launcher\\windows\\icons\\" + name + L".png";
        if (fileExists(cand)) return cand;
    }
    return s + L"icons\\" + name + L".png";
}
HFONT makeSplashFont(int size, int weight) {
    HFONT f = CreateFontW(size, 0, 0, 0, weight, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Manrope");
    if (!f) f = CreateFontW(size, 0, 0, 0, weight, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    if (!f) f = CreateFontW(size, 0, 0, 0, weight, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    return f;
}
void rotatePhrase() {
    g_splash.phraseIndex = (g_splash.phraseIndex + 1) % kPhraseCount;
    const char* text = lang(kPhraseKeys[g_splash.phraseIndex]);
    SetWindowTextW(g_splash.hStatus, fromUtf8(text).c_str());
}
void initSpinnerLUT() {
    for (int i = 0; i < SPINNER_LUT_SIZE; ++i) {
        float t = (float)i / (SPINNER_LUT_SIZE - 1);
        float angle;
        if (t < 0.07f) {
            float p = t / 0.07f; p = p * p * (3.0f - 2.0f * p);
            angle = 225.0f + (345.0f - 225.0f) * p;
        } else if (t < 0.30f) {
            float p = (t - 0.07f) / (0.30f - 0.07f);
            angle = 345.0f + (455.0f - 345.0f) * p;
        } else if (t < 0.39f) {
            float p = (t - 0.30f) / (0.39f - 0.30f); p = p * p * (3.0f - 2.0f * p);
            angle = 455.0f + (690.0f - 455.0f) * p;
        } else if (t < 0.70f) {
            float p = (t - 0.39f) / (0.70f - 0.39f);
            angle = 690.0f + (815.0f - 690.0f) * p;
        } else if (t < 0.75f) {
            float p = (t - 0.70f) / (0.75f - 0.70f); p = p * p * (3.0f - 2.0f * p);
            angle = 815.0f + (945.0f - 815.0f) * p;
        } else {
            angle = 945.0f;
        }
        g_splash.lut[i] = angle * SPINNER_PI / 180.0f;
    }
}
float evalSpinnerLUT(float t) {
    if (t <= 0) return g_splash.lut[0];
    if (t >= 1.0f) return g_splash.lut[SPINNER_LUT_SIZE - 1];
    float fi = t * (SPINNER_LUT_SIZE - 1);
    int idx = (int)fi;
    float frac = fi - idx;
    if (idx >= SPINNER_LUT_SIZE - 1) return g_splash.lut[SPINNER_LUT_SIZE - 1];
    return g_splash.lut[idx] + (g_splash.lut[idx + 1] - g_splash.lut[idx]) * frac;
}
void drawSpinner(HDC hdc, int cx, int cy, float phase) {
    Gdiplus::Graphics g(hdc);
    g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
    for (int i = 0; i < SPINNER_DOTS; ++i) {
        float dotPhase = fmod(phase - i * SPINNER_DOT_DELAY, 4.8f);
        if (dotPhase < 0) dotPhase += 4.8f;
        float t = dotPhase / 4.8f;
        if (t >= 0.76f) continue;
        float alpha = 1.0f;
        if (t < SPINNER_FADE_IN) alpha = t / SPINNER_FADE_IN;
        else if (t > 0.76f - SPINNER_FADE_OUT) alpha = (0.76f - t) / SPINNER_FADE_OUT;
        float angleRad = evalSpinnerLUT(t);
        float px = static_cast<float>(cx) + sinf(angleRad) * SPINNER_RADIUS;
        float py = static_cast<float>(cy) - cosf(angleRad) * SPINNER_RADIUS;
        BYTE a = static_cast<BYTE>(alpha * 255);
        BYTE tc = g_splash.theme.isDark ? 255 : 40;
        Gdiplus::SolidBrush brush(Gdiplus::Color(a, tc, tc, tc));
        g.FillEllipse(&brush, px - SPINNER_DOT_RADIUS, py - SPINNER_DOT_RADIUS, SPINNER_DOT_RADIUS * 2.0f, SPINNER_DOT_RADIUS * 2.0f);
    }
}
void splashWorker() {
    auto postStatus = [](const std::string& s) {
        std::string* p = new std::string(s);
        PostMessageW(g_splash.hwnd, WM_SPLASH_STATUS, 0, reinterpret_cast<LPARAM>(p));
    };
    LauncherConfig cfg = loadLauncherConfig();
    if (!cfg.checkUpdatesOnStart) {
        PostMessageW(g_splash.hwnd, WM_SPLASH_DONE, 1, 0);
        return;
    }
    std::wstring marker = joinPath(kickxDir(), L".ready");
    if (fileExists(marker)) {
        WIN32_FILE_ATTRIBUTE_DATA fad{};
        if (GetFileAttributesExW(marker.c_str(), GetFileExInfoStandard, &fad)) {
            FILETIME ft = fad.ftLastWriteTime;
            ULARGE_INTEGER now; GetSystemTimeAsFileTime(reinterpret_cast<FILETIME*>(&now));
            ULARGE_INTEGER mk; mk.LowPart = ft.dwLowDateTime; mk.HighPart = ft.dwHighDateTime;
            double hours = (double)(now.QuadPart - mk.QuadPart) / 10000000.0 / 3600.0;
            if (hours < 24.0) {
                Sleep(180);
                PostMessageW(g_splash.hwnd, WM_SPLASH_DONE, 1, 0);
                return;
            }
        }
    }
    game::ReleaseInfo release;
    std::string err;
    if (!game::fetchLatestRelease(&release, &err)) {
        Sleep(300);
        PostMessageW(g_splash.hwnd, WM_SPLASH_DONE, 1, 0);
        return;
    }
    if (release.url.empty()) {
        PostMessageW(g_splash.hwnd, WM_SPLASH_DONE, 1, 0);
        return;
    }
    std::wstring jarName = fromUtf8(release.name.empty() ? release.tag + ".jar" : release.name);
    std::wstring existing = joinPath(modsDir(), jarName);
    if (fileExists(existing)) {
        HANDLE h = CreateFileW(marker.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_HIDDEN, nullptr);
        if (h != INVALID_HANDLE_VALUE) CloseHandle(h);
        PostMessageW(g_splash.hwnd, WM_SPLASH_DONE, 1, 0);
        return;
    }
    bool installed = game::installRavex(release, &err, [](const ravex::net::Progress&) {}, nullptr);
    if (!installed) {
        Sleep(400);
    } else {
        HANDLE h = CreateFileW(marker.c_str(), GENERIC_WRITE, 0, nullptr, CREATE_ALWAYS, FILE_ATTRIBUTE_HIDDEN, nullptr);
        if (h != INVALID_HANDLE_VALUE) CloseHandle(h);
    }
    PostMessageW(g_splash.hwnd, WM_SPLASH_DONE, 1, 0);
}
LRESULT CALLBACK SplashProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_CREATE: {
            g_splash.hwnd = hwnd;
            initSpinnerLUT();
            g_splash.font = makeSplashFont(-14, FW_NORMAL);
            g_splash.titleFont = makeSplashFont(-22, FW_SEMIBOLD);
            if (g_splash.theme.bg == 0) g_splash.theme = getTheme("dark");
            if (g_splash.bgBrush) DeleteObject(g_splash.bgBrush);
            g_splash.bgBrush = CreateSolidBrush(g_splash.theme.bg);
            std::wstring logoPath = iconPathSplash(L"win10");
            if (fileExists(logoPath)) {
                g_splash.winLogo = Gdiplus::Bitmap::FromFile(logoPath.c_str());
                if (g_splash.winLogo && g_splash.winLogo->GetLastStatus() != Gdiplus::Ok) { delete g_splash.winLogo; g_splash.winLogo = nullptr; }
            }
            HINSTANCE inst = reinterpret_cast<HINSTANCE>(GetWindowLongPtrW(hwnd, GWLP_HINSTANCE));
            g_splash.hTitle = CreateWindowExW(0, L"STATIC", L"KickX Launcher", WS_CHILD | WS_VISIBLE | SS_CENTER, 0, 26, 520, 34, hwnd, nullptr, inst, nullptr);
            g_splash.hStatus = CreateWindowExW(0, L"STATIC", fromUtf8(lang(kPhraseKeys[0])).c_str(), WS_CHILD | WS_VISIBLE | SS_CENTER, 0, 150, 520, 24, hwnd, nullptr, inst, nullptr);
            SendMessageW(g_splash.hTitle, WM_SETFONT, reinterpret_cast<WPARAM>(g_splash.titleFont), TRUE);
            SendMessageW(g_splash.hStatus, WM_SETFONT, reinterpret_cast<WPARAM>(g_splash.font), TRUE);
            SetTimer(hwnd, TIMER_SPINNER, SPINNER_INTERVAL_MS, nullptr);
            SetTimer(hwnd, TIMER_PHRASE, PHRASE_INTERVAL_MS, nullptr);
            std::thread(splashWorker).detach();
            return 0;
        }
        case WM_TIMER: {
            if (wParam == TIMER_SPINNER) {
                g_splash.animPhase += 0.024f;
                if (g_splash.animPhase > 4.8f) g_splash.animPhase -= 4.8f;
                RECT rc;
                GetClientRect(hwnd, &rc);
                rc.top = SPINNER_CY - SPINNER_RADIUS - 10;
                rc.bottom = SPINNER_CY + SPINNER_RADIUS + 10;
                InvalidateRect(hwnd, &rc, FALSE);
            } else if (wParam == TIMER_PHRASE) {
                rotatePhrase();
            }
            return 0;
        }
        case WM_PAINT: {
            PAINTSTRUCT ps;
            HDC hdc = BeginPaint(hwnd, &ps);
            if (!g_splash.bgBrush) g_splash.bgBrush = CreateSolidBrush(g_splash.theme.bg);
            if (g_splash.bgBrush) FillRect(hdc, &ps.rcPaint, g_splash.bgBrush);
            SetBkMode(hdc, TRANSPARENT);
            drawSpinner(hdc, SPINNER_CX, SPINNER_CY, g_splash.animPhase);
            if (g_splash.winLogo && g_splash.winLogo->GetLastStatus() == Gdiplus::Ok) {
                Gdiplus::Graphics g(hdc);
                g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
                g.DrawImage(g_splash.winLogo, 492, 172, 20, 20);
            }
            EndPaint(hwnd, &ps);
            return 0;
        }
        case WM_CTLCOLORSTATIC: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetTextColor(dc, g_splash.theme.text);
            SetBkColor(dc, g_splash.theme.bg);
            return reinterpret_cast<LRESULT>(g_splash.bgBrush);
        }
        case WM_ERASEBKGND:
            return 1;
        case WM_SPLASH_STATUS: {
            std::string* p = reinterpret_cast<std::string*>(lParam);
            SetWindowTextW(g_splash.hStatus, fromUtf8(*p).c_str());
            delete p;
            return 0;
        }
        case WM_SPLASH_DONE: {
            g_splash.done = true;
            KillTimer(hwnd, TIMER_SPINNER);
            KillTimer(hwnd, TIMER_PHRASE);
            DestroyWindow(hwnd);
            return 0;
        }
        case WM_SYSCOMMAND: {
            if ((wParam & 0xFFF0) == SC_CLOSE) { ExitProcess(0); return 0; }
            return DefWindowProcW(hwnd, msg, wParam, lParam);
        }
        case WM_CLOSE: {
            ExitProcess(0);
            return 0;
        }
        case WM_DESTROY: {
            if (g_splash.font) DeleteObject(g_splash.font);
            if (g_splash.titleFont) DeleteObject(g_splash.titleFont);
            if (g_splash.bgBrush) DeleteObject(g_splash.bgBrush);
            if (g_splash.winLogo) delete g_splash.winLogo;
            PostQuitMessage(0);
            return 0;
        }
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}
}
bool runSplashWindow(HINSTANCE hInstance) {
    Gdiplus::GdiplusStartupInput si;
    ULONG_PTR gdiplusToken = 0;
    Gdiplus::GdiplusStartup(&gdiplusToken, &si, nullptr);
    g_splash = SplashData{};
    LauncherConfig scfg = loadLauncherConfig();
    setCurrentLanguage(scfg.language.c_str());
    g_splash.theme = getThemeForConfig(scfg.theme, scfg.customBg, scfg.customPanel, scfg.customText, scfg.customAccent);
    WNDCLASSEXW wc{};
    wc.cbSize = sizeof(wc);
    wc.lpfnWndProc = SplashProc;
    wc.hInstance = hInstance;
    wc.hIcon = LoadIconW(hInstance, MAKEINTRESOURCEW(101));
    if (!wc.hIcon) wc.hIcon = LoadIconW(nullptr, MAKEINTRESOURCEW(32512));
    wc.hIconSm = wc.hIcon;
    wc.hCursor = LoadCursorW(nullptr, MAKEINTRESOURCEW(32512));
    wc.hbrBackground = CreateSolidBrush(g_splash.theme.bg);
    wc.lpszClassName = L"KickXSplash";
    RegisterClassExW(&wc);
    int sw = 520;
    int sh = 200;
    HMONITOR mon = MonitorFromPoint({0, 0}, MONITOR_DEFAULTTOPRIMARY);
    MONITORINFO mi{};
    mi.cbSize = sizeof(mi);
    GetMonitorInfoW(mon, &mi);
    RECT wr = mi.rcWork;
    int sx = wr.left + ((wr.right - wr.left) - sw) / 2;
    int sy = wr.top + ((wr.bottom - wr.top) - sh) / 2;
    HWND hwnd = CreateWindowExW(WS_EX_TOPMOST, L"KickXSplash", L"KickX Launcher",
        WS_POPUP | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX | WS_VISIBLE, sx, sy, sw, sh, nullptr, nullptr, hInstance, nullptr);
    if (!hwnd) return true;
    RECT cr{0, 0, sw, sh};
    AdjustWindowRectEx(&cr, WS_POPUP | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX, FALSE, WS_EX_TOPMOST);
    int dw = cr.right - cr.left;
    int dh = cr.bottom - cr.top;
    SetWindowPos(hwnd, nullptr, sx, sy, dw, dh, SWP_NOZORDER);
    applyWindowTheme(hwnd, g_splash.theme);
    ShowWindow(hwnd, SW_SHOW);
    UpdateWindow(hwnd);
    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0)) {
        if (msg.message == WM_QUIT && g_splash.done) break;
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
        if (g_splash.done) {
            while (PeekMessageW(&msg, nullptr, 0, 0, PM_REMOVE)) {
                TranslateMessage(&msg);
                DispatchMessageW(&msg);
            }
            break;
        }
    }
    DeleteObject(wc.hbrBackground);
    Gdiplus::GdiplusShutdown(gdiplusToken);
    return true;
}
}
