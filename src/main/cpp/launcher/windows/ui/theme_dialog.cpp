#include "ui/include/theme_dialog.hpp"
#include "core/include/config.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "core/include/lang.hpp"
#include <windows.h>
#include <commdlg.h>
#include <commctrl.h>
#include <gdiplus.h>
#include <string>

namespace ravex::ui {

namespace {

constexpr int IDC_THEME_COMBO = 1401;
constexpr int IDC_BG_BTN = 1402;
constexpr int IDC_PANEL_BTN = 1403;
constexpr int IDC_TEXT_BTN = 1404;
constexpr int IDC_ACCENT_BTN = 1405;
constexpr int IDC_PREVIEW = 1406;

struct ThemeData {
    bool closed = false;
    bool ok = false;
    LauncherConfig cfg;
    ThemeColors cur;
    std::string selected;
    HWND combo = nullptr;
    HWND bgBtn = nullptr;
    HWND panelBtn = nullptr;
    HWND textBtn = nullptr;
    HWND accentBtn = nullptr;
    HWND preview = nullptr;
    HFONT font = nullptr;
    HFONT smallFont = nullptr;
    COLORREF custColors[16] = {};
};

HFONT makeFont(int h = -13) {
    return CreateFontW(h, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");
}

COLORREF pickColor(HWND parent, COLORREF init) {
    CHOOSECOLORW cc{};
    static COLORREF custom[16] = {RGB(255,255,255),RGB(0,0,0),RGB(90,140,255),RGB(255,0,0)};
    cc.lStructSize = sizeof(cc);
    cc.hwndOwner = parent;
    cc.rgbResult = init;
    cc.lpCustColors = custom;
    cc.Flags = CC_FULLOPEN | CC_RGBINIT;
    if (ChooseColorW(&cc)) return cc.rgbResult;
    return init;
}

void updateButtons(ThemeData* d) {
    bool isCustom = d->selected == "custom";
    EnableWindow(d->bgBtn, isCustom);
    EnableWindow(d->panelBtn, isCustom);
    EnableWindow(d->textBtn, isCustom);
    EnableWindow(d->accentBtn, isCustom);
    if (isCustom) {
        d->cur.bg = d->cfg.customBg;
        d->cur.panel = d->cfg.customPanel;
        d->cur.text = d->cfg.customText;
        d->cur.accent = d->cfg.customAccent;
    } else {
        d->cur = getTheme(d->selected);
    }
    InvalidateRect(d->preview, nullptr, TRUE);
}

void applyThemeChoice(ThemeData* d) {
    int sel = (int)SendMessageW(d->combo, CB_GETCURSEL, 0, 0);
    if (sel == 0) d->selected = "dark";
    else if (sel == 1) d->selected = "light";
    else if (sel == 2) d->selected = "midnight";
    else if (sel == 3) d->selected = "ocean";
    else if (sel == 4) d->selected = "forest";
    else d->selected = "custom";
    updateButtons(d);
}

LRESULT CALLBACK ThemeProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    ThemeData* d = reinterpret_cast<ThemeData*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
    switch (msg) {
        case WM_CREATE: {
            CREATESTRUCTW* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            d = reinterpret_cast<ThemeData*>(cs->lpCreateParams);
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(d));
            d->font = makeFont(-13);
            d->smallFont = makeFont(-11);
            HINSTANCE inst = cs->hInstance;
            CreateWindowExW(0, L"STATIC", L"Theme", WS_CHILD|WS_VISIBLE|SS_LEFT, 20, 16, 80, 18, hwnd, nullptr, inst, nullptr);
            d->combo = CreateWindowExW(0, L"COMBOBOX", nullptr, WS_CHILD|WS_VISIBLE|WS_VSCROLL|CBS_DROPDOWNLIST, 100, 14, 220, 200, hwnd, reinterpret_cast<HMENU>(IDC_THEME_COMBO), inst, nullptr);
            SendMessageW(d->combo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L"Dark"));
            SendMessageW(d->combo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L"Light"));
            SendMessageW(d->combo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L"Midnight"));
            SendMessageW(d->combo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L"Ocean"));
            SendMessageW(d->combo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L"Forest"));
            SendMessageW(d->combo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L"Custom"));
            int idx = 0;
            if (d->cfg.theme == "light") idx = 1;
            else if (d->cfg.theme == "midnight") idx = 2;
            else if (d->cfg.theme == "ocean") idx = 3;
            else if (d->cfg.theme == "forest") idx = 4;
            else if (d->cfg.theme == "custom") idx = 5;
            SendMessageW(d->combo, CB_SETCURSEL, idx, 0);
            d->selected = d->cfg.theme;
            CreateWindowExW(0, L"STATIC", L"Palette - pick colors for Custom", WS_CHILD|WS_VISIBLE|SS_LEFT, 20, 48, 300, 16, hwnd, nullptr, inst, nullptr);
            d->bgBtn = CreateWindowExW(0, L"BUTTON", L"Background", WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW, 20, 70, 150, 28, hwnd, reinterpret_cast<HMENU>(IDC_BG_BTN), inst, nullptr);
            d->panelBtn = CreateWindowExW(0, L"BUTTON", L"Panel", WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW, 190, 70, 150, 28, hwnd, reinterpret_cast<HMENU>(IDC_PANEL_BTN), inst, nullptr);
            d->textBtn = CreateWindowExW(0, L"BUTTON", L"Text", WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW, 20, 108, 150, 28, hwnd, reinterpret_cast<HMENU>(IDC_TEXT_BTN), inst, nullptr);
            d->accentBtn = CreateWindowExW(0, L"BUTTON", L"Accent", WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW, 190, 108, 150, 28, hwnd, reinterpret_cast<HMENU>(IDC_ACCENT_BTN), inst, nullptr);
            d->preview = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD|WS_VISIBLE|SS_OWNERDRAW|WS_BORDER, 20, 150, 320, 80, hwnd, reinterpret_cast<HMENU>(IDC_PREVIEW), inst, nullptr);
            HWND okBtn = CreateWindowExW(0, L"BUTTON", L"Apply", WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW, 180, 244, 70, 28, hwnd, reinterpret_cast<HMENU>(IDOK), inst, nullptr);
            HWND cancelBtn = CreateWindowExW(0, L"BUTTON", L"Cancel", WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW, 270, 244, 70, 28, hwnd, reinterpret_cast<HMENU>(IDCANCEL), inst, nullptr);
            for (HWND c : {d->combo, d->bgBtn, d->panelBtn, d->textBtn, d->accentBtn, okBtn, cancelBtn}) SendMessageW(c, WM_SETFONT, reinterpret_cast<WPARAM>(d->font), TRUE);
            updateButtons(d);
            return 0;
        }
        case WM_COMMAND:
            if (HIWORD(wParam) == CBN_SELCHANGE && LOWORD(wParam) == IDC_THEME_COMBO) { applyThemeChoice(d); return 0; }
            if (LOWORD(wParam) == IDC_BG_BTN) { COLORREF c = pickColor(hwnd, d->cfg.customBg); d->cfg.customBg = c; d->cur.bg = c; InvalidateRect(d->preview, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_PANEL_BTN) { COLORREF c = pickColor(hwnd, d->cfg.customPanel); d->cfg.customPanel = c; d->cur.panel = c; InvalidateRect(d->preview, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_TEXT_BTN) { COLORREF c = pickColor(hwnd, d->cfg.customText); d->cfg.customText = c; d->cur.text = c; InvalidateRect(d->preview, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_ACCENT_BTN) { COLORREF c = pickColor(hwnd, d->cfg.customAccent); d->cfg.customAccent = c; d->cur.accent = c; InvalidateRect(d->preview, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDOK) {
                applyThemeChoice(d);
                d->cfg.theme = d->selected;
                d->cfg.customBg = d->cur.bg;
                d->cfg.customPanel = d->cur.panel;
                d->cfg.customText = d->cur.text;
                d->cfg.customAccent = d->cur.accent;
                saveLauncherConfig(d->cfg);
                d->ok = true; d->closed = true; DestroyWindow(hwnd); return 0;
            }
            if (LOWORD(wParam) == IDCANCEL) { d->closed = true; DestroyWindow(hwnd); return 0; }
            return 0;
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds = reinterpret_cast<DRAWITEMSTRUCT*>(lParam);
            if (ds->CtlID == IDOK || ds->CtlID == IDCANCEL) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool isOk = (ds->CtlID == IDOK);
                COLORREF bg = isOk ? RGB(90,140,255) : RGB(45,45,47);
                if (ds->itemState & ODS_SELECTED) bg = isOk ? RGB(70,120,235) : RGB(60,60,62);
                else if (ds->itemState & ODS_HOTLIGHT) bg = isOk ? RGB(100,150,255) : RGB(55,55,57);
                HBRUSH bgBr = CreateSolidBrush(bg); FillRect(dc, &rc, bgBr); DeleteObject(bgBr);
                Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                Gdiplus::SolidBrush br(Gdiplus::Color(GetRValue(bg),GetGValue(bg),GetBValue(bg)));
                Gdiplus::GraphicsPath path; path.AddArc(rc.left,rc.top,8,8,180,90); path.AddArc(rc.right-8,rc.top,8,8,270,90); path.AddArc(rc.right-8,rc.bottom-8,8,8,0,90); path.AddArc(rc.left,rc.bottom-8,8,8,90,90); path.CloseFigure();
                g.FillPath(&br,&path);
                wchar_t txt[64]; GetWindowTextW(ds->hwndItem, txt, 64);
                if(wcslen(txt)>0){
                 Gdiplus::SolidBrush tbr(Gdiplus::Color(255,255,255));
                 Gdiplus::Font f(dc, d->font);
                 Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentCenter); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                 Gdiplus::RectF rf((Gdiplus::REAL)rc.left,(Gdiplus::REAL)rc.top,(Gdiplus::REAL)(rc.right-rc.left),(Gdiplus::REAL)(rc.bottom-rc.top));
                 g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                 g.DrawString(txt,-1,&f,rf,&fmt,&tbr);
                }
                return TRUE;
            }
            if (ds->CtlID == IDC_PREVIEW) {
                HDC dc = ds->hDC;
                RECT rc = ds->rcItem;
                HBRUSH bg = CreateSolidBrush(d->cur.bg);
                FillRect(dc, &rc, bg); DeleteObject(bg);
                RECT panel = {rc.left + 10, rc.top + 10, rc.right - 10, rc.top + 36};
                HBRUSH pb = CreateSolidBrush(d->cur.panel);
                FillRect(dc, &panel, pb); DeleteObject(pb);
                SetBkMode(dc, TRANSPARENT);
                SetTextColor(dc, d->cur.text);
                HFONT old = (HFONT)SelectObject(dc, d->font);
                DrawTextW(dc, L"KickX Launcher - Preview", -1, &panel, DT_CENTER|DT_VCENTER|DT_SINGLELINE);
                SelectObject(dc, old);
                RECT acc = {rc.left + 10, rc.bottom - 24, rc.right - 10, rc.bottom - 10};
                HBRUSH ab = CreateSolidBrush(d->cur.accent);
                FillRect(dc, &acc, ab); DeleteObject(ab);
                return TRUE;
            }
            return FALSE;
        }
        case WM_CTLCOLORSTATIC: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, RGB(28,28,30));
            SetTextColor(dc, RGB(225,225,225));
            return reinterpret_cast<LRESULT>(GetStockObject(BLACK_BRUSH));
        }
        case WM_ERASEBKGND: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            RECT rc; GetClientRect(hwnd,&rc);
            HBRUSH b=CreateSolidBrush(RGB(28,28,30));
            FillRect(dc,&rc,b); DeleteObject(b); return 1;
        }
        case WM_CLOSE: d->closed=true; DestroyWindow(hwnd); return 0;
        case WM_DESTROY: DeleteObject(d->font); DeleteObject(d->smallFont); d->closed=true; return 0;
        default: return DefWindowProcW(hwnd,msg,wParam,lParam);
    }
}

}

bool showThemeDialog(HWND parent) {
    HINSTANCE inst = reinterpret_cast<HINSTANCE>(GetWindowLongPtrW(parent, GWLP_HINSTANCE));
    if (!inst) inst = GetModuleHandleW(nullptr);
    static bool reg=false;
    if(!reg){ WNDCLASSEXW wc{}; wc.cbSize=sizeof(wc); wc.lpfnWndProc=ThemeProc; wc.hInstance=inst; wc.hCursor=LoadCursorW(nullptr, MAKEINTRESOURCEW(32512)); wc.hIcon=LoadIconW(inst, MAKEINTRESOURCEW(101)); if(!wc.hIcon) wc.hIcon=LoadIconW(nullptr, MAKEINTRESOURCEW(32512)); wc.hIconSm=wc.hIcon; wc.lpszClassName=L"KickxThemeDlg"; RegisterClassExW(&wc); reg=true; }
    ThemeData d;
    d.cfg = loadLauncherConfig();
    d.selected = d.cfg.theme;
    d.cur = getThemeForConfig(d.cfg.theme, d.cfg.customBg, d.cfg.customPanel, d.cfg.customText, d.cfg.customAccent);
    RECT pr{}; GetWindowRect(parent,&pr);
    int pw=pr.right-pr.left; int ph=pr.bottom-pr.top;
    int dw=370; int dh=290;
    int x=pr.left+(pw-dw)/2; int y=pr.top+(ph-dh)/2;
    HMONITOR mon=MonitorFromWindow(parent, MONITOR_DEFAULTTONEAREST); MONITORINFO mi{}; mi.cbSize=sizeof(mi); GetMonitorInfoW(mon,&mi); RECT wr=mi.rcWork;
    if(x<wr.left) x=wr.left+16; if(y<wr.top) y=wr.top+16;
    if(x+dw>wr.right) x=wr.right-dw-16; if(y+dh>wr.bottom) y=wr.bottom-dh-16;
    HWND hwnd=CreateWindowExW(0, L"KickxThemeDlg", L"Theme - Palette", WS_POPUP|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MAXIMIZEBOX, x, y, dw, dh, parent, nullptr, inst, &d);
    if(!hwnd) return false;
    applyWindowTheme(hwnd, d.cur);
    ShowWindow(hwnd, SW_SHOW); UpdateWindow(hwnd);
    EnableWindow(parent,FALSE);
    MSG msg; while(!d.closed){ while(PeekMessageW(&msg,nullptr,0,0,PM_REMOVE)){ if(IsDialogMessageW(hwnd,&msg))continue; TranslateMessage(&msg); DispatchMessageW(&msg);} Sleep(10); }
    EnableWindow(parent,TRUE);
    RedrawWindow(parent, nullptr, nullptr, RDW_INVALIDATE|RDW_ALLCHILDREN|RDW_UPDATENOW|RDW_FRAME);
    SetForegroundWindow(parent);
    return d.ok;
}

}
