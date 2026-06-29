#include <windows.h>
#include <commctrl.h>
#include <jni.h>
#include "calculator.h"
#include <string>
#include <vector>
#include <deque>
#include <mutex>
#include <thread>
#include <sstream>
#include <cstdio>

#pragma comment(lib, "comctl32.lib")
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "user32.lib")



static const COLORREF CLR_BG       = RGB(0x0e,0x0a,0x1a);
static const COLORREF CLR_DISPLAY  = RGB(0xee,0xee,0xee);
static const COLORREF CLR_RESULT   = RGB(0xda,0x70,0xd6);
static const COLORREF CLR_ERROR    = RGB(0xff,0x66,0x66);
static const COLORREF CLR_TITLE    = RGB(0xda,0x70,0xd6);
static const COLORREF CLR_HIST     = RGB(0x7a,0x6a,0x9c);
static const COLORREF CLR_BORDER   = RGB(0x28,0x1a,0x4a);
static const COLORREF CLR_BTN_NUM  = RGB(0x1a,0x10,0x40);
static const COLORREF CLR_BTN_NUM_H= RGB(0x2a,0x20,0x60);
static const COLORREF CLR_BTN_OP   = RGB(0x1e,0x0e,0x38);
static const COLORREF CLR_BTN_FN   = RGB(0x0e,0x0e,0x26);
static const COLORREF CLR_BTN_FN_H = RGB(0x1a,0x18,0x40);
static const COLORREF CLR_BTN_EQ   = RGB(0x7a,0x1a,0xaa);
static const COLORREF CLR_BTN_EQ_H = RGB(0x9a,0x2a,0xcc);
static const COLORREF CLR_BTN_CLR  = RGB(0x3a,0x0a,0x1a);
static const COLORREF CLR_BTN_CLR_H= RGB(0x5a,0x1a,0x2a);
static const COLORREF CLR_BTN_TXT  = RGB(0xee,0xee,0xee);
static const COLORREF CLR_FN_TXT   = RGB(0x90,0x90,0xd0);
static const COLORREF CLR_OP_TXT   = RGB(0xda,0x70,0xd6);
static const COLORREF CLR_ERR_TXT  = RGB(0xff,0x88,0x88);

static const int WIN_CX = 400;
static const int WIN_CY = 580;
static const int BTN_GAP = 4;
static const int BTN_W  = 64;
static const int BTN_H  = 36;
static const int FN_COLS = 5;
static const int FN_ROWS = 2;
static const int FN_BTN_H = 28;



static JavaVM*   g_jvm       = nullptr;
static jclass    g_calc_class = nullptr;
static HWND      g_main_wnd  = nullptr;
static HINSTANCE g_hinst     = nullptr;
static HFONT     g_disp_font = nullptr;
static HFONT     g_res_font  = nullptr;
static HFONT     g_btn_font  = nullptr;
static HFONT     g_fn_font   = nullptr;
static HFONT     g_title_font= nullptr;

static std::string  g_expr;
static std::deque<std::string> g_history;
static std::mutex   g_mutex;

static HWND g_hDisplay = nullptr;
static HWND g_hResult  = nullptr;
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



static void update_display() {
    if (g_hDisplay)
        SetWindowTextW(g_hDisplay, to_wide(g_expr.empty() ? "" : g_expr).c_str());
    if (g_hResult && !g_expr.empty()) {
        std::string preview = ravex::MathParser::evaluate(g_expr);
        if (preview.substr(0, 6) == "Error:")
            SetWindowTextW(g_hResult, L"");
        else
            SetWindowTextW(g_hResult, to_wide("= " + preview).c_str());
    } else if (g_hResult) {
        SetWindowTextW(g_hResult, L"");
    }
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
    bool isError = (res.substr(0, 6) == "Error:");
    if (isError) {
        if (g_hResult) SetWindowTextW(g_hResult, to_wide(res).c_str());
        return;
    }
    std::string entry = g_expr + " = " + res;
    g_history.push_front(entry);
    if (g_history.size() > 50) g_history.pop_back();
    g_expr = res;
    if (g_hDisplay) SetWindowTextW(g_hDisplay, to_wide(g_expr).c_str());
    if (g_hResult)  SetWindowTextW(g_hResult, L"");
    refresh_history();
}

static void handle_calc(wchar_t ch) {
    if ((ch >= L'0' && ch <= L'9') || ch == L'.' || ch == L'+' || ch == L'-' ||
        ch == L'*' || ch == L'/' || ch == L'%' || ch == L'^' || ch == L'(' || ch == L')') {
        g_expr += (char)ch;
        update_display();
    } else if (ch == L'\r' || ch == L'\n') {
        do_calculate();
    } else if (ch == 27) {
        g_expr.clear();
        if (g_hResult) SetWindowTextW(g_hResult, L"");
        update_display();
    } else if (ch == 8) {
        if (!g_expr.empty()) {
            size_t pos = g_expr.size() - 1;
            while (pos > 0 && (g_expr[pos] & 0xC0) == 0x80) pos--;
            g_expr.erase(g_expr.begin() + pos, g_expr.end());
        }
        update_display();
    }
}



static std::string btn_action(HWND hBtn) {
    wchar_t buf[64] = {0};
    GetWindowTextW(hBtn, buf, 64);
    
    std::wstring w(buf);
    if (w == L"\u00b1")    return "\xc2\xb1";     
    if (w == L"\u2190" || w == L"\u232b") return "BS";
    if (w == L"\u00d7")    return "*";
    if (w == L"\u00f7")    return "/";
    if (w == L"\u2212")    return "-";
    if (w == L"\u03c0")    return "pi";
    return to_utf8(w);
}



static int g_btn_id = 100;

static void add_btn(HWND parent, const wchar_t* text, int x, int y, int w, int h) {
    CreateWindowExW(0, L"BUTTON", text,
        WS_CHILD | WS_VISIBLE | BS_OWNERDRAW,
        x, y, w, h, parent, (HMENU)(INT_PTR)(g_btn_id++), g_hinst, nullptr);
}



static LRESULT CALLBACK CalcWndProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
    case WM_CREATE: {
        int y = 8;
        
        CreateWindowExW(0, L"STATIC", L"\u26a1 RaveX Calculator",
            WS_CHILD | WS_VISIBLE | SS_LEFT, 10, y, 300, 22,
            hWnd, nullptr, g_hinst, nullptr);
        y += 28;

        
        g_hDisplay = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
            WS_CHILD | WS_VISIBLE | ES_READONLY | ES_RIGHT | ES_AUTOHSCROLL,
            8, y, WIN_CX - 16, 28, hWnd, nullptr, g_hinst, nullptr);
        y += 34;

        
        g_hResult = CreateWindowExW(0, L"STATIC", L"",
            WS_CHILD | WS_VISIBLE | SS_RIGHT,
            8, y, WIN_CX - 16, 18, hWnd, nullptr, g_hinst, nullptr);
        y += 24;

        y += 4;

        
        const wchar_t* fns[FN_ROWS][FN_COLS] = {
            {L"sin(", L"cos(", L"tan(", L"sqrt(", L"log("},
            {L"asin(",L"acos(",L"atan(",L"^",     L"fact("}
        };
        int fn_btn_w = (WIN_CX - 16 - BTN_GAP * (FN_COLS - 1)) / FN_COLS;
        for (int r = 0; r < FN_ROWS; r++)
            for (int c = 0; c < FN_COLS; c++)
                add_btn(hWnd, fns[r][c], 8 + c * (fn_btn_w + BTN_GAP),
                        y + r * (FN_BTN_H + BTN_GAP), fn_btn_w, FN_BTN_H);

        y += FN_ROWS * (FN_BTN_H + BTN_GAP) + 4;

        
        int grid_w = 4 * BTN_W + 3 * BTN_GAP;
        int gx = (WIN_CX - grid_w) / 2;

        
        add_btn(hWnd, L"C",     gx, y, BTN_W, BTN_H);
        add_btn(hWnd, L"\u00b1", gx + (BTN_W+BTN_GAP)*1, y, BTN_W, BTN_H);
        add_btn(hWnd, L"%",     gx + (BTN_W+BTN_GAP)*2, y, BTN_W, BTN_H);
        add_btn(hWnd, L"\u2190", gx + (BTN_W+BTN_GAP)*3, y, BTN_W, BTN_H);
        y += BTN_H + BTN_GAP;

        
        add_btn(hWnd, L"7", gx, y, BTN_W, BTN_H);
        add_btn(hWnd, L"8", gx + (BTN_W+BTN_GAP)*1, y, BTN_W, BTN_H);
        add_btn(hWnd, L"9", gx + (BTN_W+BTN_GAP)*2, y, BTN_W, BTN_H);
        add_btn(hWnd, L"/", gx + (BTN_W+BTN_GAP)*3, y, BTN_W, BTN_H);
        y += BTN_H + BTN_GAP;

        
        add_btn(hWnd, L"4", gx, y, BTN_W, BTN_H);
        add_btn(hWnd, L"5", gx + (BTN_W+BTN_GAP)*1, y, BTN_W, BTN_H);
        add_btn(hWnd, L"6", gx + (BTN_W+BTN_GAP)*2, y, BTN_W, BTN_H);
        add_btn(hWnd, L"*", gx + (BTN_W+BTN_GAP)*3, y, BTN_W, BTN_H);
        y += BTN_H + BTN_GAP;

        
        add_btn(hWnd, L"1", gx, y, BTN_W, BTN_H);
        add_btn(hWnd, L"2", gx + (BTN_W+BTN_GAP)*1, y, BTN_W, BTN_H);
        add_btn(hWnd, L"3", gx + (BTN_W+BTN_GAP)*2, y, BTN_W, BTN_H);
        add_btn(hWnd, L"-", gx + (BTN_W+BTN_GAP)*3, y, BTN_W, BTN_H);
        y += BTN_H + BTN_GAP;

        
        add_btn(hWnd, L"0", gx, y, BTN_W, BTN_H);
        add_btn(hWnd, L".", gx + (BTN_W+BTN_GAP)*1, y, BTN_W, BTN_H);
        add_btn(hWnd, L"(", gx + (BTN_W+BTN_GAP)*2, y, BTN_W, BTN_H);
        add_btn(hWnd, L"+", gx + (BTN_W+BTN_GAP)*3, y, BTN_W, BTN_H);
        y += BTN_H + BTN_GAP;

        
        add_btn(hWnd, L")",      gx, y, BTN_W, BTN_H);
        add_btn(hWnd, L"\u03c0", gx + (BTN_W+BTN_GAP)*1, y, BTN_W, BTN_H);
        add_btn(hWnd, L"=",      gx + (BTN_W+BTN_GAP)*2, y, BTN_W * 2 + BTN_GAP, BTN_H);
        
        y += BTN_H + BTN_GAP + 4;

        
        CreateWindowExW(0, L"STATIC", L"History",
            WS_CHILD | WS_VISIBLE | SS_LEFT, 10, y, 100, 16,
            hWnd, nullptr, g_hinst, nullptr);
        y += 20;

        g_hHistory = CreateWindowExW(WS_EX_CLIENTEDGE, L"LISTBOX", L"",
            WS_CHILD | WS_VISIBLE | WS_VSCROLL | LBS_NOINTEGRALHEIGHT,
            8, y, WIN_CX - 16, WIN_CY - y - 8,
            hWnd, nullptr, g_hinst, nullptr);

        
        if (g_hDisplay && g_disp_font) SendMessageW(g_hDisplay, WM_SETFONT, (WPARAM)g_disp_font, TRUE);
        if (g_hResult  && g_res_font)  SendMessageW(g_hResult,  WM_SETFONT, (WPARAM)g_res_font,  TRUE);
        if (g_hHistory && g_fn_font)   SendMessageW(g_hHistory, WM_SETFONT, (WPARAM)g_fn_font,   TRUE);

        return 0;
    }

    case WM_COMMAND: {
        if (HIWORD(wParam) == BN_CLICKED) {
            HWND hBtn = (HWND)lParam;
            std::string a = btn_action(hBtn);

            if (a == "=") {
                do_calculate();
            } else if (a == "C") {
                g_expr.clear();
                if (g_hResult) SetWindowTextW(g_hResult, L"");
                update_display();
            } else if (a == "BS") {
                if (!g_expr.empty()) {
                    size_t pos = g_expr.size() - 1;
                    while (pos > 0 && (g_expr[pos] & 0xC0) == 0x80) pos--;
                    g_expr.erase(g_expr.begin() + pos, g_expr.end());
                }
                update_display();
            } else if (a == "\xc2\xb1") {
                if (!g_expr.empty()) {
                    if (g_expr[0] == '-') g_expr = g_expr.substr(1);
                    else g_expr = "-" + g_expr;
                }
                update_display();
            } else if (a == "pi") {
                g_expr += "pi";
                update_display();
            } else {
                g_expr += a;
                update_display();
            }
        }
        return 0;
    }

    case WM_CHAR: {
        handle_calc((wchar_t)wParam);
        return 0;
    }

    case WM_CTLCOLORSTATIC: {
        HDC hdc = (HDC)wParam;
        HWND hw = (HWND)lParam;
        if (hw == g_hResult) {
            SetBkColor(hdc, RGB(0x08,0x06,0x14));
            SetTextColor(hdc, CLR_RESULT);
            static HBRUSH br = nullptr;
            if (!br) br = CreateSolidBrush(RGB(0x08,0x06,0x14));
            return (LRESULT)br;
        }
        SetBkColor(hdc, CLR_BG);
        SetTextColor(hdc, CLR_TITLE);
        static HBRUSH br2 = nullptr;
        if (!br2) br2 = CreateSolidBrush(CLR_BG);
        return (LRESULT)br2;
    }

    case WM_CTLCOLOREDIT: {
        HDC hdc = (HDC)wParam;
        HWND hw = (HWND)lParam;
        if (hw == g_hDisplay) {
            SetBkColor(hdc, RGB(0x08,0x06,0x14));
            SetTextColor(hdc, CLR_DISPLAY);
            static HBRUSH br = nullptr;
            if (!br) br = CreateSolidBrush(RGB(0x08,0x06,0x14));
            return (LRESULT)br;
        }
        return DefWindowProcW(hWnd, msg, wParam, lParam);
    }

    case WM_CTLCOLORLISTBOX: {
        HDC hdc = (HDC)wParam;
        SetBkColor(hdc, RGB(0x08,0x06,0x14));
        SetTextColor(hdc, CLR_HIST);
        static HBRUSH br = nullptr;
        if (!br) br = CreateSolidBrush(RGB(0x08,0x06,0x14));
        return (LRESULT)br;
    }

    case WM_DRAWITEM: {
        LPDRAWITEMSTRUCT dis = (LPDRAWITEMSTRUCT)lParam;
        if (dis->CtlType != ODT_BUTTON) break;

        wchar_t txt[64] = {0};
        GetWindowTextW(dis->hwndItem, txt, 64);
        std::wstring wlabel(txt);
        std::string  label = to_utf8(wlabel);

        bool hover  = (dis->itemState & ODS_HOTLIGHT) || (dis->itemState & ODS_SELECTED);
        bool sel    = (dis->itemState & ODS_SELECTED) != 0;

        COLORREF bg, fg;

        if (label == "C")               { bg = sel ? CLR_BTN_CLR_H : CLR_BTN_CLR; fg = CLR_ERR_TXT; }
        else if (label == "=")          { bg = sel ? CLR_BTN_EQ_H : CLR_BTN_EQ;  fg = CLR_BTN_TXT; }
        else if (wlabel == L"\u00b1" || label == "%" || wlabel == L"\u2190") {
                                            bg = sel ? RGB(0x3a,0x1a,0x60) : CLR_BTN_OP; fg = CLR_OP_TXT; }
        else if (label == "+" || label == "-" || label == "*" || label == "/" ||
                 label == "(" || label == ")" || label == "^") {
                                            bg = sel ? RGB(0x3a,0x1a,0x60) : CLR_BTN_OP; fg = CLR_OP_TXT; }
        else if (wlabel == L"\u03c0")   { bg = sel ? RGB(0x3a,0x1a,0x60) : CLR_BTN_OP; fg = CLR_OP_TXT; }
        else if (label == "sin(" || label == "cos(" || label == "tan(" ||
                 label == "asin(" || label == "acos(" || label == "atan(" ||
                 label == "sqrt(" || label == "log(" || label == "fact(") {
                                            bg = sel ? CLR_BTN_FN_H : CLR_BTN_FN; fg = CLR_FN_TXT; }
        else                            { bg = (hover || sel) ? CLR_BTN_NUM_H : CLR_BTN_NUM; fg = CLR_BTN_TXT; }

        HBRUSH br = CreateSolidBrush(bg);
        FillRect(dis->hDC, &dis->rcItem, br);
        DeleteObject(br);

        HPEN pen = CreatePen(PS_SOLID, 1, CLR_BORDER);
        HGDIOBJ old_pen = SelectObject(dis->hDC, pen);
        HGDIOBJ old_br_ = SelectObject(dis->hDC, GetStockObject(NULL_BRUSH));
        Rectangle(dis->hDC, dis->rcItem.left, dis->rcItem.top,
                  dis->rcItem.right, dis->rcItem.bottom);
        SelectObject(dis->hDC, old_pen);
        SelectObject(dis->hDC, old_br_);
        DeleteObject(pen);

        SetTextColor(dis->hDC, fg);
        SetBkMode(dis->hDC, TRANSPARENT);
        HFONT use_font = g_fn_font;
        if (label == "sin(" || label == "cos(" || label == "tan(" ||
            label == "asin(" || label == "acos(" || label == "atan(" ||
            label == "sqrt(" || label == "log(" || label == "fact(")
            use_font = g_fn_font;
        else
            use_font = g_btn_font;
        HFONT old_f = (HFONT)SelectObject(dis->hDC, use_font);
        RECT tr = dis->rcItem;
        DrawTextW(dis->hDC, txt, -1, &tr, DT_CENTER | DT_VCENTER | DT_SINGLELINE | DT_NOPREFIX);
        SelectObject(dis->hDC, old_f);

        return TRUE;
    }

    case WM_CLOSE:
        DestroyWindow(hWnd);
        return 0;

    case WM_DESTROY: {
        g_main_wnd = nullptr;
        notify_java_close();
        PostQuitMessage(0);
        return 0;
    }
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

    g_title_font = make_font(16, FW_BOLD, L"Segoe UI");
    g_disp_font  = make_font(20, FW_NORMAL, L"Consolas");
    g_res_font   = make_font(14, FW_NORMAL, L"Consolas");
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

    g_main_wnd = CreateWindowExW(WS_EX_WINDOWEDGE,
        L"RavexCalculator", L"RaveX Calculator",
        WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX,
        CW_USEDEFAULT, CW_USEDEFAULT, WIN_CX, WIN_CY,
        nullptr, nullptr, g_hinst, nullptr);

    if (g_main_wnd) {
        ShowWindow(g_main_wnd, SW_SHOW);
        UpdateWindow(g_main_wnd);

        MSG msg;
        while (GetMessageW(&msg, nullptr, 0, 0)) {
            TranslateMessage(&msg);
            DispatchMessageW(&msg);
        }
    }

    if (g_title_font) { DeleteObject(g_title_font); g_title_font = nullptr; }
    if (g_disp_font)  { DeleteObject(g_disp_font);  g_disp_font  = nullptr; }
    if (g_res_font)   { DeleteObject(g_res_font);   g_res_font   = nullptr; }
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
    g_expr.clear();
    g_history.clear();
    std::thread(calc_thread).detach();
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_Calculator_closeCalculator(JNIEnv* env, jclass cls) {
    HWND hw = g_main_wnd;
    if (hw) PostMessageW(hw, WM_CLOSE, 0, 0);
}

JNIEXPORT jstring JNICALL
Java_ravex_modules_client_Calculator_nativeEvaluate(JNIEnv* env, jclass cls, jstring expr) {
    const char* e = env->GetStringUTFChars(expr, nullptr);
    std::string result = ravex::MathParser::evaluate(std::string(e));
    env->ReleaseStringUTFChars(expr, e);
    return env->NewStringUTF(result.c_str());
}

}
