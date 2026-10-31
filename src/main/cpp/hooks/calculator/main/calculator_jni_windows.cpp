#include <windows.h>
#include <windowsx.h>
#include <commctrl.h>
#include <jni.h>
#include "../../../common/include/jni_raii.hpp"
#include "calculator.hpp"
#include <string>
#include <vector>
#include <deque>
#include <mutex>
#include <thread>
#include <sstream>
#include <cstdio>
#include <cmath>

#pragma comment(lib, "comctl32.lib")
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "user32.lib")
#pragma comment(lib, "msimg32.lib")

using ravex::jni::JniStringUtfChars;
using ravex::jni::checkAndClearException;

// same palette as the desktop dashboard
static const COLORREF CLR_BG        = RGB(0xf0,0xf0,0xf0);
static const COLORREF CLR_MENU_BG   = RGB(0xf5,0xf6,0xf7);
static const COLORREF CLR_MENU_LINE = RGB(0xdc,0xdc,0xdc);
static const COLORREF CLR_MENU_TXT  = RGB(0x1f,0x1f,0x1f);
static const COLORREF CLR_DISP_BG   = RGB(0xff,0xff,0xff);
static const COLORREF CLR_DISP_TXT  = RGB(0x1f,0x1f,0x1f);
static const COLORREF CLR_DISP_BRD  = RGB(0xb4,0xbe,0xc6);
static const COLORREF CLR_KEY_T     = RGB(0xf8,0xfc,0xff);
static const COLORREF CLR_KEY_B     = RGB(0xd0,0xe2,0xf3);
static const COLORREF CLR_KEY_BRD   = RGB(0x9d,0xad,0xbc);
static const COLORREF CLR_FN_T      = RGB(0xf2,0xf6,0xf9);
static const COLORREF CLR_FN_B      = RGB(0xd9,0xe4,0xec);
static const COLORREF CLR_FN_BRD    = RGB(0x9d,0xad,0xbc);
static const COLORREF CLR_KEY_TXT   = RGB(0x1f,0x2b,0x36);
static const COLORREF CLR_FN_TXT    = RGB(0x4a,0x53,0x5c);
static const COLORREF CLR_HOV_T     = RGB(0xea,0xf6,0xfd);
static const COLORREF CLR_HOV_B     = RGB(0xc9,0xe6,0xf7);
static const COLORREF CLR_HOV_BRD   = RGB(0x3c,0x7f,0xb1);
static const COLORREF CLR_PRS_T     = RGB(0xc9,0xe6,0xf7);
static const COLORREF CLR_PRS_B     = RGB(0xa9,0xd2,0xee);
static const COLORREF CLR_PRS_BRD   = RGB(0x2c,0x62,0x8b);
static const COLORREF CLR_EQ_T      = RGB(0x3c,0x7f,0xb1);
static const COLORREF CLR_EQ_B      = RGB(0x3c,0x7f,0xb1);
static const COLORREF CLR_EQ_BRD    = RGB(0x2f,0x6e,0x9e);
static const COLORREF CLR_EQ_HOV    = RGB(0x4a,0x90,0xc4);
static const COLORREF CLR_EQ_PRS    = RGB(0x2c,0x62,0x8b);
static const COLORREF CLR_EQ_TXT    = RGB(0xff,0xff,0xff);
static const COLORREF CLR_C_TXT     = RGB(0xa8,0x30,0x30);
static const COLORREF CLR_PREVIEW   = RGB(0x5f,0x6b,0x76);
static const COLORREF CLR_HIST_TXT  = RGB(0x33,0x33,0x33);
static const COLORREF CLR_TITLE     = RGB(0x1f,0x2b,0x36);
static const COLORREF CLR_TITLE_T   = RGB(0xf8,0xfc,0xff);
static const COLORREF CLR_TITLE_B   = RGB(0xd0,0xe2,0xf3);
static const COLORREF CLR_TITLE_BRD = RGB(0x9d,0xad,0xbc);
static const COLORREF CLR_CLOSE_T   = RGB(0xdc,0x62,0x5a);
static const COLORREF CLR_CLOSE_B   = RGB(0xb1,0x2b,0x24);

static const int WIN_CX = 400;
static const int WIN_CY = 592;
static const int TITLE_H = 32;
static const int MENU_H = 22;
static const int BTN_GAP = 4;
static const int BTN_W  = 64;
static const int BTN_H  = 36;
static const int FN_BTN_H = 30;
static const int COLS = 5;
static const int DISP_H = 54;
static const int PREVIEW_H = 18;
static const int CAP_BTN_W = 30;
static const int CAP_CLOSE_W = 46;

enum { CAP_MIN = 0, CAP_CLOSE = 1 };

struct CalcButton {
    std::wstring label;
    RECT rc{};
    bool fn = false;
};

static JavaVM*   g_jvm       = nullptr;
static jclass    g_calc_class = nullptr;
static HWND      g_main_wnd  = nullptr;
static HINSTANCE g_hinst     = nullptr;
static HFONT     g_disp_font = nullptr;
static HFONT     g_prev_font = nullptr;
static HFONT     g_btn_font  = nullptr;
static HFONT     g_fn_font   = nullptr;
static HFONT     g_menu_font = nullptr;
static HFONT     g_title_font = nullptr;
static HICON     g_title_icon = nullptr;

static std::vector<CalcButton> g_buttons;
static int g_hover = -1;
static int g_pressed = -1;
static int g_hover_cap = -1;

static std::string  g_expr;
static std::string  g_preview;
static std::deque<std::string> g_history;
static std::mutex   g_mutex;

static RECT g_display_rect{};
static RECT g_preview_rect{};
static RECT g_history_label{};
static HWND g_hHistory = nullptr;

static std::wstring to_wide(const std::string& s) {
    if (s.empty()) return L"";
    int n = MultiByteToWideChar(CP_UTF8, 0, s.c_str(), (int)s.size(), nullptr, 0);
    std::wstring w(n, L'\0');
    MultiByteToWideChar(CP_UTF8, 0, s.c_str(), (int)s.size(), &w[0], n);
    return w;
}

static std::string to_utf8(const std::wstring& w) {
    if (w.empty()) return "";
    int n = WideCharToMultiByte(CP_UTF8, 0, w.c_str(), (int)w.size(), nullptr, 0, nullptr, nullptr);
    std::string s(n, '\0');
    WideCharToMultiByte(CP_UTF8, 0, w.c_str(), (int)w.size(), &s[0], n, nullptr, nullptr);
    return s;
}

static JNIEnv* get_env() {
    JNIEnv* env = nullptr;
    if (g_jvm && g_jvm->GetEnv((void**)&env, JNI_VERSION_1_8) == JNI_EDETACHED) {
        g_jvm->AttachCurrentThread((void**)&env, nullptr);
    }
    return env;
}

static void notify_java_close() {
    JNIEnv* env = get_env();
    if (!env || !g_calc_class) return;
    jmethodID m = env->GetStaticMethodID(g_calc_class, "onNativeClose", "()V");
    if (m) env->CallStaticVoidMethod(g_calc_class, m);
}

static void refresh_preview() {
    if (g_expr.empty()) {
        g_preview.clear();
        return;
    }
    std::string result = ravex::MathParser::evaluate(g_expr);
    g_preview = (result.substr(0, 6) == "Error:") ? "" : ("= " + result);
}

static void refresh_history() {
    if (!g_hHistory) return;
    SendMessageW(g_hHistory, LB_RESETCONTENT, 0, 0);
    for (const auto& entry : g_history)
        SendMessageW(g_hHistory, LB_ADDSTRING, 0, (LPARAM)to_wide(entry).c_str());
}

static void do_calculate() {
    if (g_expr.empty()) return;
    std::string res = ravex::MathParser::evaluate(g_expr);
    if (res.substr(0, 6) == "Error:") {
        g_preview = res;
        return;
    }
    g_history.push_front(g_expr + " = " + res);
    if (g_history.size() > 50) g_history.pop_back();
    g_expr = res;
    refresh_history();
    refresh_preview();
}

static void backspace_expr() {
    if (g_expr.empty()) return;
    size_t pos = g_expr.size() - 1;
    while (pos > 0 && (g_expr[pos] & 0xC0) == 0x80) pos--;
    g_expr.erase(g_expr.begin() + pos, g_expr.end());
}

static void perform_action(const std::wstring& label) {
    std::string a = to_utf8(label);
    if (a == "=") {
        do_calculate();
    } else if (a == "C") {
        g_expr.clear();
        g_preview.clear();
    } else if (a == "\xe2\x86\x90") {
        backspace_expr();
        refresh_preview();
    } else if (a == "\xc2\xb1") {
        if (!g_expr.empty()) {
            if (g_expr[0] == '-') g_expr = g_expr.substr(1);
            else g_expr = "-" + g_expr;
        }
        refresh_preview();
    } else if (a == "\xcf\x80") {
        g_expr += "pi";
        refresh_preview();
    } else {
        g_expr += a;
        refresh_preview();
    }
}

static void handle_calc(wchar_t ch) {
    if ((ch >= L'0' && ch <= L'9') || ch == L'.' || ch == L'+' || ch == L'-' ||
        ch == L'*' || ch == L'/' || ch == L'%' || ch == L'^' || ch == L'(' || ch == L')') {
        g_expr += (char)ch;
        refresh_preview();
    } else if (ch == L'\r' || ch == L'\n') {
        do_calculate();
    } else if (ch == 27) {
        g_expr.clear();
        g_preview.clear();
    } else if (ch == 8) {
        backspace_expr();
        refresh_preview();
    }
}

static void fill_rect(HDC dc, const RECT& rc, COLORREF color) {
    HBRUSH brush = CreateSolidBrush(color);
    FillRect(dc, &rc, brush);
    DeleteObject(brush);
}

static void draw_text(HDC dc, const RECT& rc, const std::wstring& text, COLORREF color, HFONT font, UINT align) {
    SetTextColor(dc, color);
    SetBkMode(dc, TRANSPARENT);
    HFONT old = (HFONT)SelectObject(dc, font);
    RECT r = rc;
    DrawTextW(dc, text.c_str(), (int)text.size(), &r, align | DT_SINGLELINE | DT_VCENTER | DT_NOPREFIX);
    SelectObject(dc, old);
}

static void gradient_fill_border(HDC dc, const RECT& rc, COLORREF top, COLORREF bottom, COLORREF border) {
    TRIVERTEX vtx[2] = {
        { rc.left + 1, rc.top + 1, (COLOR16)(GetRValue(top) << 8), (COLOR16)(GetGValue(top) << 8), (COLOR16)(GetBValue(top) << 8), 0xff00 },
        { rc.right - 1, rc.bottom - 1, (COLOR16)(GetRValue(bottom) << 8), (COLOR16)(GetGValue(bottom) << 8), (COLOR16)(GetBValue(bottom) << 8), 0xff00 }
    };
    GRADIENT_RECT gr = {0, 1};
    GradientFill(dc, vtx, 2, &gr, 1, GRADIENT_FILL_RECT_V);

    HPEN pen = CreatePen(PS_SOLID, 1, border);
    HGDIOBJ op = SelectObject(dc, pen);
    HGDIOBJ ob = SelectObject(dc, GetStockObject(NULL_BRUSH));
    Rectangle(dc, rc.left, rc.top, rc.right, rc.bottom);
    SelectObject(dc, op);
    SelectObject(dc, ob);
    DeleteObject(pen);
}

// rounded key, gradient body plus border
static void draw_key(HDC dc, const RECT& rc, COLORREF top, COLORREF bottom, COLORREF border) {
    HRGN rgn = CreateRoundRectRgn(rc.left, rc.top, rc.right + 1, rc.bottom + 1, 9, 9);
    int saved = SaveDC(dc);
    SelectClipRgn(dc, rgn);

    TRIVERTEX vtx[2] = {
        { rc.left + 1, rc.top + 1, (COLOR16)(GetRValue(top) << 8), (COLOR16)(GetGValue(top) << 8), (COLOR16)(GetBValue(top) << 8), 0xff00 },
        { rc.right - 1, rc.bottom - 1, (COLOR16)(GetRValue(bottom) << 8), (COLOR16)(GetGValue(bottom) << 8), (COLOR16)(GetBValue(bottom) << 8), 0xff00 }
    };
    GRADIENT_RECT gr = {0, 1};
    GradientFill(dc, vtx, 2, &gr, 1, GRADIENT_FILL_RECT_V);
    RestoreDC(dc, saved);
    DeleteObject(rgn);

    HPEN pen = CreatePen(PS_SOLID, 1, border);
    HGDIOBJ old_pen = SelectObject(dc, pen);
    HGDIOBJ old_br = SelectObject(dc, GetStockObject(NULL_BRUSH));
    RoundRect(dc, rc.left, rc.top, rc.right, rc.bottom, 9, 9);
    SelectObject(dc, old_pen);
    SelectObject(dc, old_br);
    DeleteObject(pen);
}

static int caption_button_at(int x, int y, int cx) {
    int by = (TITLE_H - 20) / 2;
    if (y < by || y > by + 20) return -1;
    int right = cx - 2;
    if (x >= right - CAP_CLOSE_W) return CAP_CLOSE;
    if (x >= right - CAP_CLOSE_W - CAP_BTN_W) return CAP_MIN;
    return -1;
}

static void draw_caption_button(HDC dc, int x, int which, bool hover) {
    int y = (TITLE_H - 20) / 2;
    int w = (which == CAP_CLOSE) ? CAP_CLOSE_W : CAP_BTN_W;
    RECT rc = {x, y, x + w, y + 20};
    COLORREF top = CLR_TITLE_T, bottom = CLR_TITLE_B, border = CLR_TITLE_BRD;
    if (hover) {
        if (which == CAP_CLOSE) { top = CLR_CLOSE_T; bottom = CLR_CLOSE_B; border = CLR_CLOSE_B; }
        else { top = CLR_HOV_T; bottom = CLR_HOV_B; border = CLR_HOV_BRD; }
    }
    gradient_fill_border(dc, rc, top, bottom, border);

    COLORREF fg = (hover && which == CAP_CLOSE) ? RGB(0xff,0xff,0xff) : CLR_TITLE;
    int mx = x + w / 2;
    int my = y + 10;
    HPEN pen = CreatePen(PS_SOLID, 1, fg);
    HGDIOBJ op = SelectObject(dc, pen);
    if (which == CAP_MIN) {
        MoveToEx(dc, mx - 5, my + 4, nullptr);
        LineTo(dc, mx + 5, my + 4);
    } else {
        MoveToEx(dc, mx - 5, my - 5, nullptr);
        LineTo(dc, mx + 5, my + 5);
        MoveToEx(dc, mx + 5, my - 5, nullptr);
        LineTo(dc, mx - 5, my + 5);
    }
    SelectObject(dc, op);
    DeleteObject(pen);
}

static void layout_window() {
    g_buttons.clear();

    int top = TITLE_H + MENU_H + 8;
    g_display_rect = {8, top, WIN_CX - 8, top + DISP_H};
    top += DISP_H + 4;
    g_preview_rect = {8, top, WIN_CX - 8, top + PREVIEW_H};
    top += PREVIEW_H + 8;

    int grid_w = COLS * BTN_W + (COLS - 1) * BTN_GAP;
    int gx = (WIN_CX - grid_w) / 2;
    auto col = [](int i) { return i * (BTN_W + BTN_GAP); };

    const wchar_t* fn_rows[2][COLS] = {
        {L"sin(", L"cos(", L"tan(", L"sqrt(", L"log("},
        {L"asin(",L"acos(",L"atan(",L"^",     L"fact("}
    };
    int y = top;
    for (int r = 0; r < 2; r++) {
        for (int c = 0; c < COLS; c++) {
            CalcButton b;
            b.label = fn_rows[r][c];
            b.rc = {gx + col(c), y + r * (FN_BTN_H + BTN_GAP),
                    gx + col(c) + BTN_W, y + r * (FN_BTN_H + BTN_GAP) + FN_BTN_H};
            b.fn = true;
            g_buttons.push_back(b);
        }
    }
    y += 2 * (FN_BTN_H + BTN_GAP) + 6;

    auto add = [&](const wchar_t* label, int cx, int cy, int w, int h) {
        CalcButton b;
        b.label = label;
        b.rc = {cx, cy, cx + w, cy + h};
        g_buttons.push_back(b);
    };

    const wchar_t* row3[COLS] = {L"C", L"\u00b1", L"\u2190", L"(", L")"};
    const wchar_t* row4[COLS] = {L"7", L"8", L"9", L"/", L"%"};
    const wchar_t* row5[COLS] = {L"4", L"5", L"6", L"*", L"\u03c0"};
    const wchar_t* row6[COLS] = {L"1", L"2", L"3", L"-", nullptr};
    for (int c = 0; c < COLS; c++) add(row3[c], gx + col(c), y, BTN_W, BTN_H);
    y += BTN_H + BTN_GAP;
    for (int c = 0; c < COLS; c++) add(row4[c], gx + col(c), y, BTN_W, BTN_H);
    y += BTN_H + BTN_GAP;
    for (int c = 0; c < COLS; c++) add(row5[c], gx + col(c), y, BTN_W, BTN_H);
    y += BTN_H + BTN_GAP;
    for (int c = 0; c < 4; c++) add(row6[c], gx + col(c), y, BTN_W, BTN_H);
    add(L"=", gx + col(4), y, BTN_W, BTN_H * 2 + BTN_GAP);
    y += BTN_H + BTN_GAP;
    add(L"0", gx + col(0), y, BTN_W * 2 + BTN_GAP, BTN_H);
    add(L".", gx + col(2), y, BTN_W, BTN_H);
    add(L"+", gx + col(3), y, BTN_W, BTN_H);
    y += BTN_H + BTN_GAP + 6;

    g_history_label = {12, y, 112, y + 16};
    y += 18;
    if (g_hHistory) {
        MoveWindow(g_hHistory, 8, y, WIN_CX - 16, WIN_CY - y - 8, TRUE);
    }
}

static int hit_test(int x, int y) {
    for (size_t i = 0; i < g_buttons.size(); i++) {
        const RECT& rc = g_buttons[i].rc;
        if (x >= rc.left && x < rc.right && y >= rc.top && y < rc.bottom) return (int) i;
    }
    return -1;
}

static void apply_app_icon(HWND hwnd) {
    HMODULE self = nullptr;
    if (!GetModuleHandleExW(GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS | GET_MODULE_HANDLE_EX_FLAG_UNCHANGED_REFCOUNT,
            (LPCWSTR)&apply_app_icon, &self)) return;
    HICON big = (HICON)LoadImageW(self, MAKEINTRESOURCEW(1), IMAGE_ICON, 32, 32, LR_DEFAULTCOLOR);
    g_title_icon = (HICON)LoadImageW(self, MAKEINTRESOURCEW(1), IMAGE_ICON, 16, 16, LR_DEFAULTCOLOR);
    if (big) SendMessageW(hwnd, WM_SETICON, ICON_BIG, (LPARAM)big);
    if (g_title_icon) SendMessageW(hwnd, WM_SETICON, ICON_SMALL, (LPARAM)g_title_icon);
}

static void paint_window(HWND hWnd) {
    PAINTSTRUCT ps;
    HDC dc = BeginPaint(hWnd, &ps);

    HDC mem = CreateCompatibleDC(dc);
    HBITMAP bmp = CreateCompatibleBitmap(dc, WIN_CX, WIN_CY);
    HGDIOBJ old_bmp = SelectObject(mem, bmp);

    RECT full = {0, 0, WIN_CX, WIN_CY};
    fill_rect(mem, full, CLR_BG);

    RECT title = {0, 0, WIN_CX, TITLE_H};
    gradient_fill_border(mem, title, CLR_TITLE_T, CLR_TITLE_B, CLR_TITLE_BRD);
    if (g_title_icon) {
        DrawIconEx(mem, 8, (TITLE_H - 16) / 2, g_title_icon, 16, 16, 0, nullptr, DI_NORMAL);
    }
    RECT title_text = {30, 0, WIN_CX - 90, TITLE_H};
    draw_text(mem, title_text, L"Calculator", CLR_TITLE, g_title_font, DT_LEFT);

    int capRight = WIN_CX - 2;
    draw_caption_button(mem, capRight - CAP_CLOSE_W, CAP_CLOSE, g_hover_cap == CAP_CLOSE);
    draw_caption_button(mem, capRight - CAP_CLOSE_W - CAP_BTN_W, CAP_MIN, g_hover_cap == CAP_MIN);

    RECT menu = {0, TITLE_H, WIN_CX, TITLE_H + MENU_H};
    fill_rect(mem, menu, CLR_MENU_BG);
    HPEN line = CreatePen(PS_SOLID, 1, CLR_MENU_LINE);
    HGDIOBJ old_pen = SelectObject(mem, line);
    MoveToEx(mem, 0, TITLE_H + MENU_H - 1, nullptr);
    LineTo(mem, WIN_CX, TITLE_H + MENU_H - 1);
    SelectObject(mem, old_pen);
    DeleteObject(line);
    const wchar_t* items[] = {L"View", L"Edit", L"Help"};
    int mx = 8;
    for (auto* item : items) {
        RECT tr = {mx, TITLE_H, mx + 70, TITLE_H + MENU_H};
        draw_text(mem, tr, item, CLR_MENU_TXT, g_menu_font, DT_LEFT);
        mx += 48;
    }

    fill_rect(mem, g_display_rect, CLR_DISP_BG);
    HPEN disp_pen = CreatePen(PS_SOLID, 1, CLR_DISP_BRD);
    old_pen = SelectObject(mem, disp_pen);
    HGDIOBJ old_br = SelectObject(mem, GetStockObject(NULL_BRUSH));
    Rectangle(mem, g_display_rect.left, g_display_rect.top, g_display_rect.right, g_display_rect.bottom);
    SelectObject(mem, old_pen);
    SelectObject(mem, old_br);
    DeleteObject(disp_pen);

    std::wstring expr = to_wide(g_expr);
    RECT expr_rc = g_display_rect;
    expr_rc.right -= 8;
    // clip long expressions from the left
    HDC tmp = CreateCompatibleDC(dc);
    HFONT old_tmp = (HFONT)SelectObject(tmp, g_disp_font);
    while (expr.size() > 1) {
        SIZE sz{};
        GetTextExtentPoint32W(tmp, expr.c_str(), (int)expr.size(), &sz);
        if ((int)sz.cx <= (expr_rc.right - expr_rc.left) - 16) break;
        expr.erase(expr.begin());
    }
    SelectObject(tmp, old_tmp);
    DeleteDC(tmp);
    draw_text(mem, expr_rc, expr, CLR_DISP_TXT, g_disp_font, DT_RIGHT);

    draw_text(mem, g_preview_rect, to_wide(g_preview), CLR_PREVIEW, g_prev_font, DT_RIGHT);

    for (size_t i = 0; i < g_buttons.size(); i++) {
        const CalcButton& b = g_buttons[i];
        bool pressed = (int) i == g_pressed;
        bool hover = (int) i == g_hover || pressed;
        std::string label = to_utf8(b.label);
        bool is_eq = label == "=";
        bool is_c = label == "C";

        COLORREF top = CLR_KEY_T, bottom = CLR_KEY_B, border = CLR_KEY_BRD;
        if (b.fn) { top = CLR_FN_T; bottom = CLR_FN_B; border = CLR_FN_BRD; }
        if (is_eq) { top = CLR_EQ_T; bottom = CLR_EQ_B; border = CLR_EQ_BRD; }
        if (is_eq) {
            if (pressed) { top = bottom = border = CLR_EQ_PRS; }
            else if (hover) { top = bottom = border = CLR_EQ_HOV; }
        } else {
            if (hover) { top = CLR_HOV_T; bottom = CLR_HOV_B; border = CLR_HOV_BRD; }
            if (pressed) { top = CLR_PRS_T; bottom = CLR_PRS_B; border = CLR_PRS_BRD; }
        }

        draw_key(mem, b.rc, top, bottom, border);

        COLORREF fg = CLR_KEY_TXT;
        if (b.fn) fg = CLR_FN_TXT;
        if (is_c) fg = CLR_C_TXT;
        if (is_eq) fg = CLR_EQ_TXT;
        RECT trect = b.rc;
        if (pressed) OffsetRect(&trect, 0, 1);
        draw_text(mem, trect, b.label, fg, b.fn ? g_fn_font : g_btn_font, DT_CENTER);
    }

    draw_text(mem, g_history_label, L"History", CLR_MENU_TXT, g_menu_font, DT_LEFT);

    // outer 1px frame, no window border
    old_pen = SelectObject(mem, CreatePen(PS_SOLID, 1, CLR_TITLE_BRD));
    old_br = SelectObject(mem, GetStockObject(NULL_BRUSH));
    Rectangle(mem, 0, 0, WIN_CX, WIN_CY);
    SelectObject(mem, old_pen);
    SelectObject(mem, old_br);
    DeleteObject(old_pen);

    BitBlt(dc, 0, 0, WIN_CX, WIN_CY, mem, 0, 0, SRCCOPY);
    SelectObject(mem, old_bmp);
    DeleteObject(bmp);
    DeleteDC(mem);
    EndPaint(hWnd, &ps);
}

static LRESULT CALLBACK CalcWndProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
    case WM_CREATE: {
        g_hHistory = CreateWindowExW(0, L"LISTBOX", L"",
            WS_CHILD | WS_VISIBLE | WS_VSCROLL | LBS_NOINTEGRALHEIGHT | WS_BORDER,
            8, 0, WIN_CX - 16, 100, hWnd, nullptr, g_hinst, nullptr);
        if (g_hHistory && g_prev_font) SendMessageW(g_hHistory, WM_SETFONT, (WPARAM)g_prev_font, TRUE);
        layout_window();
        return 0;
    }

    case WM_SIZE:
        layout_window();
        InvalidateRect(hWnd, nullptr, FALSE);
        return 0;

    case WM_ERASEBKGND:
        return 1;

    case WM_PAINT:
        paint_window(hWnd);
        return 0;

    case WM_NCHITTEST: {
        LRESULT hit = DefWindowProcW(hWnd, msg, wParam, lParam);
        if (hit != HTCLIENT) return hit;
        POINT pt = {GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam)};
        ScreenToClient(hWnd, &pt);
        if (pt.y >= 0 && pt.y < TITLE_H) {
            if (caption_button_at(pt.x, pt.y, WIN_CX) >= 0) return HTCLIENT;
            return HTCAPTION;
        }
        return HTCLIENT;
    }

    case WM_MOUSEMOVE: {
        int x = GET_X_LPARAM(lParam);
        int y = GET_Y_LPARAM(lParam);
        int cap = caption_button_at(x, y, WIN_CX);
        int hit = (y < TITLE_H) ? -1 : hit_test(x, y);
        if (hit != g_hover || cap != g_hover_cap) {
            g_hover = hit;
            g_hover_cap = cap;
            InvalidateRect(hWnd, nullptr, FALSE);
        }
        TRACKMOUSEEVENT tme = {sizeof(tme), TME_LEAVE, hWnd, 0};
        TrackMouseEvent(&tme);
        return 0;
    }

    case WM_MOUSELEAVE:
        if (g_hover != -1 || g_hover_cap != -1) {
            g_hover = -1;
            g_hover_cap = -1;
            InvalidateRect(hWnd, nullptr, FALSE);
        }
        return 0;

    case WM_LBUTTONDOWN: {
        int x = GET_X_LPARAM(lParam);
        int y = GET_Y_LPARAM(lParam);
        int cap = caption_button_at(x, y, WIN_CX);
        if (cap >= 0) {
            if (cap == CAP_MIN) ShowWindow(hWnd, SW_MINIMIZE);
            else PostMessageW(hWnd, WM_CLOSE, 0, 0);
            return 0;
        }
        int hit = hit_test(x, y);
        g_pressed = hit;
        if (hit >= 0) {
            SetCapture(hWnd);
            InvalidateRect(hWnd, nullptr, FALSE);
        }
        return 0;
    }

    case WM_LBUTTONUP: {
        int hit = hit_test(GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam));
        int pressed = g_pressed;
        g_pressed = -1;
        if (GetCapture() == hWnd) ReleaseCapture();
        InvalidateRect(hWnd, nullptr, FALSE);
        if (pressed >= 0 && pressed == hit) {
            perform_action(g_buttons[pressed].label);
            InvalidateRect(hWnd, nullptr, FALSE);
        }
        return 0;
    }

    case WM_MOUSEWHEEL:
        return 0;

    case WM_CHAR:
        handle_calc((wchar_t)wParam);
        InvalidateRect(hWnd, nullptr, FALSE);
        return 0;

    case WM_CTLCOLORLISTBOX: {
        HDC hdc = (HDC)wParam;
        SetBkColor(hdc, CLR_DISP_BG);
        SetTextColor(hdc, CLR_HIST_TXT);
        static HBRUSH br = nullptr;
        if (!br) br = CreateSolidBrush(CLR_DISP_BG);
        return (LRESULT)br;
    }

    case WM_CLOSE:
        DestroyWindow(hWnd);
        return 0;

    case WM_DESTROY:
        g_main_wnd = nullptr;
        notify_java_close();
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProcW(hWnd, msg, wParam, lParam);
}

static void calc_thread() {
    CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);

    INITCOMMONCONTROLSEX icex;
    icex.dwSize = sizeof(icex);
    icex.dwICC = ICC_STANDARD_CLASSES;
    InitCommonControlsEx(&icex);

    g_hinst = GetModuleHandleW(nullptr);

    auto make_font = [](int h, int weight, const wchar_t* face) -> HFONT {
        return CreateFontW(h, 0, 0, 0, weight, FALSE, FALSE, FALSE,
            DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
            CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, face);
    };

    g_menu_font  = make_font(13, FW_NORMAL, L"Segoe UI");
    g_title_font = make_font(14, FW_SEMIBOLD, L"Segoe UI");
    g_disp_font  = make_font(26, FW_NORMAL, L"Segoe UI");
    g_prev_font  = make_font(13, FW_NORMAL, L"Segoe UI");
    g_btn_font   = make_font(15, FW_NORMAL, L"Segoe UI");
    g_fn_font    = make_font(12, FW_NORMAL, L"Segoe UI");

    WNDCLASSEXW wc = {sizeof(wc)};
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = CalcWndProc;
    wc.hInstance = g_hinst;
    wc.hCursor = LoadCursor(nullptr, IDC_ARROW);
    wc.hbrBackground = CreateSolidBrush(CLR_BG);
    wc.lpszClassName = L"RavexCalculator";
    RegisterClassExW(&wc);

    // borderless, title bar and buttons are ours
    g_main_wnd = CreateWindowExW(WS_EX_APPWINDOW,
        L"RavexCalculator", L"Calculator",
        WS_POPUP | WS_MINIMIZEBOX,
        CW_USEDEFAULT, CW_USEDEFAULT, WIN_CX, WIN_CY,
        nullptr, nullptr, g_hinst, nullptr);

    if (g_main_wnd) {
        apply_app_icon(g_main_wnd);
        ShowWindow(g_main_wnd, SW_SHOW);
        SetForegroundWindow(g_main_wnd);
        UpdateWindow(g_main_wnd);

        MSG msg;
        while (GetMessageW(&msg, nullptr, 0, 0)) {
            TranslateMessage(&msg);
            DispatchMessageW(&msg);
        }
    }

    if (g_menu_font)  { DeleteObject(g_menu_font);  g_menu_font  = nullptr; }
    if (g_title_font) { DeleteObject(g_title_font); g_title_font = nullptr; }
    if (g_disp_font)  { DeleteObject(g_disp_font);  g_disp_font  = nullptr; }
    if (g_prev_font)  { DeleteObject(g_prev_font);  g_prev_font  = nullptr; }
    if (g_btn_font)   { DeleteObject(g_btn_font);   g_btn_font   = nullptr; }
    if (g_fn_font)    { DeleteObject(g_fn_font);    g_fn_font    = nullptr; }
    g_main_wnd = nullptr;
    CoUninitialize();
}

extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_Calculator_openCalculator(JNIEnv* env, jclass cls) {
    if (g_calc_class) env->DeleteGlobalRef(g_calc_class);
    g_calc_class = (jclass)env->NewGlobalRef(cls);
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_expr.clear();
        g_preview.clear();
        g_history.clear();
    }
    std::thread(calc_thread).detach();
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_Calculator_closeCalculator(JNIEnv* env, jclass cls) {
    HWND hw = g_main_wnd;
    if (hw) PostMessageW(hw, WM_CLOSE, 0, 0);
}

JNIEXPORT jstring JNICALL
Java_ravex_modules_client_Calculator_nativeEvaluate(JNIEnv* env, jclass cls, jstring expr) {
    JniStringUtfChars eGuard(env, expr);
    if (!eGuard) return env->NewStringUTF("");
    if (checkAndClearException(env)) return nullptr;
    std::string result = ravex::MathParser::evaluate(std::string(eGuard.get()));
    jstring out = env->NewStringUTF(result.c_str());
    if (checkAndClearException(env)) return nullptr;
    return out;
}

}
