#include "ui/include/console_window.hpp"
#include "core/include/config.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "ui/include/glow.hpp"
#include <windows.h>
#include <windowsx.h>
#include <commctrl.h>
#include <string>
#include <vector>
#include <functional>
#include <fstream>

namespace ravex::ui {

namespace {

constexpr int IDC_CONSOLE_EDIT = 1201;
constexpr int IDC_KILL_BTN = 1202;
constexpr int IDC_COPY_BTN = 1203;

struct ConsoleData {
    HWND hwnd = nullptr;
    HWND hEdit = nullptr;
    HWND hKillBtn = nullptr;
    HFONT font = nullptr;
    HBRUSH bgBrush = nullptr;
    HBRUSH panelBrush = nullptr;
    ThemeColors theme;
    std::function<void()> killCallback;
    bool closed = false;
    HWND hoveredBtn = nullptr;
    TRACKMOUSEEVENT tme{};
    GlowData glow;
    std::wstring logFile;
    bool saveLogs = true;
};

ConsoleData g_console;
bool g_consoleCreated = false;

HFONT makeFont() {
    return CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       DEFAULT_PITCH | FF_DONTCARE, L"Consolas");
}

void drawKillButton(ConsoleData& data, DRAWITEMSTRUCT* ds) {
    bool hovered = (ds->hwndItem == data.hoveredBtn);
    bool pressed = (ds->itemState & ODS_SELECTED) != 0;
    bool disabled = (ds->itemState & ODS_DISABLED) != 0;
    COLORREF bg;
    if (disabled) {
        bg = data.theme.bg;
    } else if (hovered) {
        bg = RGB(180, 40, 40);
    } else if (pressed) {
        bg = RGB(140, 30, 30);
    } else {
        bg = RGB(120, 30, 30);
    }
    COLORREF fg = disabled ? RGB(100, 100, 100) : RGB(255, 255, 255);
    HBRUSH br = CreateSolidBrush(bg);
    FillRect(ds->hDC, &ds->rcItem, br);
    DeleteObject(br);
    HPEN pen = CreatePen(PS_SOLID, 1, hovered ? RGB(200, 60, 60) : RGB(80, 20, 20));
    HBRUSH oldB = (HBRUSH)SelectObject(ds->hDC, GetStockObject(NULL_BRUSH));
    HPEN oldP = (HPEN)SelectObject(ds->hDC, pen);
    Rectangle(ds->hDC, ds->rcItem.left, ds->rcItem.top, ds->rcItem.right, ds->rcItem.bottom);
    SelectObject(ds->hDC, oldB);
    SelectObject(ds->hDC, oldP);
    DeleteObject(pen);
    SetBkMode(ds->hDC, TRANSPARENT);
    SetTextColor(ds->hDC, fg);
    HFONT oldF = (HFONT)SelectObject(ds->hDC, data.font);
    wchar_t txt[64] = {0};
    GetWindowTextW(ds->hwndItem, txt, 64);
    DrawTextW(ds->hDC, txt, -1, &ds->rcItem, DT_CENTER | DT_VCENTER | DT_SINGLELINE);
    SelectObject(ds->hDC, oldF);
}

LRESULT CALLBACK ConsoleProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    ConsoleData* data = &g_console;
    switch (msg) {
        case WM_CREATE:
            data->font = makeFont();
            data->bgBrush = CreateSolidBrush(data->theme.bg);
            data->panelBrush = CreateSolidBrush(data->theme.panel);
            data->hEdit = CreateWindowExW(0, L"EDIT", L"",
                                          WS_CHILD | WS_VISIBLE | WS_BORDER | WS_VSCROLL |
                                          ES_MULTILINE | ES_READONLY | ES_AUTOVSCROLL,
                                          8, 8, 600, 340, hwnd, reinterpret_cast<HMENU>(IDC_CONSOLE_EDIT),
                                          GetModuleHandleW(nullptr), nullptr);
            data->hKillBtn = CreateWindowExW(0, L"BUTTON", L"Kill Game",
                                             WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                             516, 356, 92, 30, hwnd, reinterpret_cast<HMENU>(IDC_KILL_BTN),
                                             GetModuleHandleW(nullptr), nullptr);
            CreateWindowExW(0, L"BUTTON", L"Copy",
                             WS_CHILD | WS_VISIBLE | WS_TABSTOP,
                             410, 356, 92, 30, hwnd, reinterpret_cast<HMENU>(IDC_COPY_BTN),
                             GetModuleHandleW(nullptr), nullptr);
            SendMessageW(data->hEdit, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            SendMessageW(data->hEdit, EM_SETLIMITTEXT, 1024 * 1024, 0);
            glowCreate(hwnd, &data->glow);
            glowSetButtons(&data->glow, {data->hKillBtn});
            SetTimer(hwnd, 99, 16, nullptr);
            return 0;
        case WM_COMMAND:
            if (LOWORD(wParam) == IDC_KILL_BTN) {
                if (data->killCallback) data->killCallback();
                return 0;
            }
            if (LOWORD(wParam) == IDC_COPY_BTN) {
                SendMessageW(data->hEdit, EM_SETSEL, 0, -1);
                SendMessageW(data->hEdit, WM_COPY, 0, 0);
                SendMessageW(data->hEdit, EM_SETSEL, -1, -1);
                return 0;
            }
            return 0;
        case WM_MOUSEMOVE: {
            POINT pt;
            pt.x = GET_X_LPARAM(lParam);
            pt.y = GET_Y_LPARAM(lParam);
            ClientToScreen(hwnd, &pt);
            HWND oldHover = data->hoveredBtn;
            data->hoveredBtn = nullptr;
            RECT rc;
            GetWindowRect(data->hKillBtn, &rc);
            if (PtInRect(&rc, pt)) data->hoveredBtn = data->hKillBtn;
            if (oldHover != data->hoveredBtn) {
                if (oldHover) InvalidateRect(oldHover, nullptr, FALSE);
                if (data->hoveredBtn) InvalidateRect(data->hoveredBtn, nullptr, FALSE);
            }
            data->tme = {};
            data->tme.cbSize = sizeof(TRACKMOUSEEVENT);
            data->tme.dwFlags = TME_LEAVE;
            data->tme.hwndTrack = hwnd;
            TrackMouseEvent(&data->tme);
            glowUpdate(&data->glow);
            return 0;
        }
        case WM_TIMER:
            if (wParam == 99) glowUpdate(&data->glow);
            return 0;
        case WM_MOUSELEAVE: {
            HWND old = data->hoveredBtn;
            data->hoveredBtn = nullptr;
            if (old) InvalidateRect(old, nullptr, FALSE);
            return 0;
        }
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds = reinterpret_cast<DRAWITEMSTRUCT*>(lParam);
            if (ds->CtlType == ODT_BUTTON && ds->CtlID == IDC_KILL_BTN) {
                drawKillButton(*data, ds);
                return TRUE;
            }
            return FALSE;
        }
        case WM_CTLCOLORSTATIC:
        case WM_CTLCOLORBTN: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, data->theme.bg);
            SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(data->bgBrush);
        }
        case WM_CTLCOLOREDIT: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, data->theme.panel);
            SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(data->panelBrush);
        }
        case WM_ERASEBKGND: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            RECT rc;
            GetClientRect(hwnd, &rc);
            FillRect(dc, &rc, data->bgBrush);
            return 1;
        }
        case WM_CLOSE:
            data->closed = true;
            ShowWindow(hwnd, SW_HIDE);
            return 0;
        case WM_DESTROY:
            data->closed = true;
            KillTimer(hwnd, 99);
            glowDestroy(&data->glow);
            if (data->font) DeleteObject(data->font);
            if (data->bgBrush) DeleteObject(data->bgBrush);
            if (data->panelBrush) DeleteObject(data->panelBrush);
            g_consoleCreated = false;
            return 0;
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

}

void openConsole(const std::function<void()>& killCallback, const std::wstring& logFile, bool saveLogs) {
    g_console.killCallback = killCallback;
    g_console.logFile = logFile;
    g_console.saveLogs = saveLogs;
    g_console.theme = getThemeForConfig(
        loadLauncherConfig().theme, 0, 0, 0, 0);
    if (g_consoleCreated && g_console.hwnd) {
        SetWindowTextW(g_console.hEdit, L"");
        if (g_console.bgBrush) DeleteObject(g_console.bgBrush);
        g_console.bgBrush = CreateSolidBrush(g_console.theme.bg);
        if (g_console.panelBrush) DeleteObject(g_console.panelBrush);
        g_console.panelBrush = CreateSolidBrush(g_console.theme.panel);
        applyWindowTheme(g_console.hwnd, g_console.theme);
        ShowWindow(g_console.hwnd, SW_SHOW);
        SetForegroundWindow(g_console.hwnd);
        return;
    }
    static bool registered = false;
    if (!registered) {
        WNDCLASSEXW wc{};
        wc.cbSize = sizeof(wc);
        wc.lpfnWndProc = ConsoleProc;
        wc.hInstance = GetModuleHandleW(nullptr);
        wc.hIcon = LoadIconW(GetModuleHandleW(nullptr), MAKEINTRESOURCEW(101));
        if (!wc.hIcon) wc.hIcon = LoadIconW(nullptr, MAKEINTRESOURCEW(32512));
        wc.hIconSm = wc.hIcon;
        wc.hCursor = LoadCursorW(nullptr, MAKEINTRESOURCEW(32512));
        wc.lpszClassName = L"RavexConsole";
        RegisterClassExW(&wc);
        registered = true;
    }
    g_console.hwnd = CreateWindowExW(0, L"RavexConsole", L"RaveX Console",
                                     WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN,
                                     CW_USEDEFAULT, CW_USEDEFAULT, 632, 430,
                                     nullptr, nullptr, GetModuleHandleW(nullptr), nullptr);
    if (!g_console.hwnd) return;
    applyWindowTheme(g_console.hwnd, g_console.theme);
    g_consoleCreated = true;
    ShowWindow(g_console.hwnd, SW_SHOW);
    UpdateWindow(g_console.hwnd);
}

void closeConsole() {
    if (g_console.hwnd) {
        DestroyWindow(g_console.hwnd);
        g_console.hwnd = nullptr;
        g_consoleCreated = false;
    }
}

void appendConsole(const std::string& line) {
    if (!g_console.hEdit) return;
    int len = GetWindowTextLengthW(g_console.hEdit);
    std::wstring wline = fromUtf8(line);
    wline += L"\r\n";
    SendMessageW(g_console.hEdit, EM_SETSEL, len, len);
    SendMessageW(g_console.hEdit, EM_REPLACESEL, FALSE, reinterpret_cast<LPARAM>(wline.c_str()));
    SendMessageW(g_console.hEdit, EM_SETSEL, -1, -1);
    SendMessageW(g_console.hEdit, EM_SCROLLCARET, 0, 0);
    if (g_console.saveLogs && !g_console.logFile.empty()) {
        std::string narrowLog = toUtf8(g_console.logFile); std::ofstream ofs(narrowLog, std::ios::app);
        if (ofs.is_open()) {
            ofs << line << "\n";
        }
    }
}

}
