#include <windows.h>
#include <windowsx.h>
#include <commctrl.h>
#include <jni.h>
#include <string>
#include <vector>
#include <map>
#include <mutex>
#include <thread>
#include <sstream>
#include <algorithm>
#include <cstdio>

#include "desktopgui.h"

#pragma comment(lib, "comctl32.lib")
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "user32.lib")



static const COLORREF CLR_BG        = RGB(0x0e,0x0a,0x1a);
static const COLORREF CLR_ROW       = RGB(0x16,0x10,0x26);
static const COLORREF CLR_ROW_HOVER = RGB(0x1e,0x15,0x34);
static const COLORREF CLR_TEXT      = RGB(0xee,0xee,0xee);
static const COLORREF CLR_ON        = RGB(0xda,0x70,0xd6);
static const COLORREF CLR_OFF       = RGB(0x4a,0x42,0x5c);
static const COLORREF CLR_GEAR      = RGB(0x6a,0x5a,0x8c);
static const COLORREF CLR_GEAR_HVR  = RGB(0xda,0x70,0xd6);
static const COLORREF CLR_BORDER    = RGB(0x28,0x1a,0x4a);
static const COLORREF CLR_TITLE     = RGB(0xda,0x70,0xd6);
static const COLORREF CLR_BTN_TXT   = RGB(0xee,0xee,0xee);
static const COLORREF CLR_DLG_BG    = RGB(0x12,0x0e,0x20);

static const int ROW_H   = 36;
static const int TITLE_H = 40;
static const int PAD     = 6;
static const int GEAR_W  = 28;
static const int TOG_W   = 54;
static const int WIN_CX  = 380;
static const int WIN_CY  = 520;



static JavaVM*   g_jvm          = nullptr;
static jclass    g_desktopgui_class = nullptr;

static HWND      g_main_wnd     = nullptr;
static HINSTANCE g_hinst        = nullptr;
static HFONT     g_font         = nullptr;
static HFONT     g_font_bold    = nullptr;

static std::vector<ravex::ModuleGuiData> g_modules;
static std::map<std::string,bool>        g_enabled;
static std::mutex g_mutex;

static int g_scroll_y  = 0;
static int g_cx        = WIN_CX;
static int g_cy        = WIN_CY;
static int g_hover_row = -1;
static bool g_hover_gear = false;
static int g_total_rows   = 0;



#define WM_UPDATE_TOGGLE (WM_APP + 100)
#define WM_UPDATE_ALL    (WM_APP + 101)

struct UpdateToggleData {
    std::string name;
    bool enabled;
};



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

static void notify_java_toggle(const std::string& name) {
    JNIEnv* env = get_env();
    if (!env || !g_desktopgui_class) return;
    jmethodID m = env->GetStaticMethodID(g_desktopgui_class, "toggleModuleFromNative", "(Ljava/lang/String;)V");
    if (m) {
        jstring jn = env->NewStringUTF(name.c_str());
        env->CallStaticVoidMethod(g_desktopgui_class, m, jn);
        env->DeleteLocalRef(jn);
    }
}

static void notify_java_close() {
    JNIEnv* env = get_env();
    if (!env || !g_desktopgui_class) return;
    jmethodID m = env->GetStaticMethodID(g_desktopgui_class, "onNativeClose", "()V");
    if (m) env->CallStaticVoidMethod(g_desktopgui_class, m);
}

static std::string notify_java_get_params(const std::string& name) {
    JNIEnv* env = get_env();
    if (!env || !g_desktopgui_class) return "";
    jmethodID m = env->GetStaticMethodID(g_desktopgui_class, "getModuleParams", "(Ljava/lang/String;)Ljava/lang/String;");
    if (m) {
        jstring jn = env->NewStringUTF(name.c_str());
        jstring jr = (jstring)env->CallStaticObjectMethod(g_desktopgui_class, m, jn);
        env->DeleteLocalRef(jn);
        if (jr) {
            const char* c = env->GetStringUTFChars(jr, nullptr);
            std::string out(c);
            env->ReleaseStringUTFChars(jr, c);
            env->DeleteLocalRef(jr);
            return out;
        }
    }
    return "";
}

static void notify_java_set_param(const std::string& mod, const std::string& param, const std::string& val) {
    JNIEnv* env = get_env();
    if (!env || !g_desktopgui_class) return;
    jmethodID m = env->GetStaticMethodID(g_desktopgui_class, "setModuleParam", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
    if (m) {
        jstring jm = env->NewStringUTF(mod.c_str());
        jstring jp = env->NewStringUTF(param.c_str());
        jstring jv = env->NewStringUTF(val.c_str());
        env->CallStaticVoidMethod(g_desktopgui_class, m, jm, jp, jv);
        env->DeleteLocalRef(jm);
        env->DeleteLocalRef(jp);
        env->DeleteLocalRef(jv);
    }
}



static void fill_rect(HDC dc, int x, int y, int w, int h, COLORREF c) {
    RECT r = {x, y, x + w, y + h};
    HBRUSH br = CreateSolidBrush(c);
    FillRect(dc, &r, br);
    DeleteObject(br);
}

static void draw_rounded_rect(HDC dc, int x, int y, int w, int h, COLORREF fill, COLORREF border) {
    HBRUSH br_fill = CreateSolidBrush(fill);
    HPEN   pen     = CreatePen(PS_SOLID, 1, border);
    HGDIOBJ old_br = SelectObject(dc, br_fill);
    HGDIOBJ old_pn = SelectObject(dc, pen);
    RoundRect(dc, x, y, x + w, y + h, 6, 6);
    SelectObject(dc, old_br);
    SelectObject(dc, old_pn);
    DeleteObject(pen);
    DeleteObject(br_fill);
}

static void draw_text(HDC dc, int x, int y, int w, int h, const std::string& text, COLORREF color, HFONT font, UINT align) {
    SetTextColor(dc, color);
    SetBkMode(dc, TRANSPARENT);
    HFONT old = (HFONT)SelectObject(dc, font);
    std::wstring wtext = to_wide(text);
    RECT r = {x, y, x + w, y + h};
    DrawTextW(dc, wtext.c_str(), (int)wtext.size(), &r, align | DT_SINGLELINE | DT_VCENTER | DT_NOPREFIX);
    SelectObject(dc, old);
}

static void draw_text_w(HDC dc, int x, int y, int w, int h, const std::wstring& text, COLORREF color, HFONT font, UINT align) {
    SetTextColor(dc, color);
    SetBkMode(dc, TRANSPARENT);
    HFONT old = (HFONT)SelectObject(dc, font);
    RECT r = {x, y, x + w, y + h};
    DrawTextW(dc, text.c_str(), (int)text.size(), &r, align | DT_SINGLELINE | DT_VCENTER | DT_NOPREFIX);
    SelectObject(dc, old);
}



struct ParamCtrl {
    std::string name;
    std::string type;
    HWND hwnd;
    std::string str_val;
    double num_val, num_min, num_max, num_step;
    std::vector<std::string> modes;
    int ctrl_id;
};

struct ParamDialogData {
    std::string mod_name;
    std::vector<ParamCtrl> ctrls;
    int next_id = 2000;
};

static LRESULT CALLBACK ParamDlgProc(HWND hDlg, UINT msg, WPARAM wParam, LPARAM lParam) {
    auto data = (ParamDialogData*)GetWindowLongPtrW(hDlg, GWLP_USERDATA);

    switch (msg) {
    case WM_CREATE: {
        auto cs = (CREATESTRUCTW*)lParam;
        std::string params_spec = to_utf8(std::wstring((const wchar_t*)cs->lpCreateParams));
        auto d = new ParamDialogData();
        d->mod_name = params_spec.substr(0, params_spec.find('\n'));
        std::string spec = params_spec.substr(params_spec.find('\n') + 1);

        SetWindowLongPtrW(hDlg, GWLP_USERDATA, (LONG_PTR)d);

        
        std::string remaining = spec;
        std::vector<std::string> specs;
        size_t pos;
        while ((pos = remaining.find('|')) != std::string::npos) {
            specs.push_back(remaining.substr(0, pos));
            remaining.erase(0, pos + 1);
        }
        if (!remaining.empty()) specs.push_back(remaining);

        int y = 8;
        int cw, ch;
        {
            RECT rc;
            GetClientRect(hDlg, &rc);
            cw = rc.right - rc.left;
            ch = rc.bottom - rc.top;
        }

        for (const auto& ps : specs) {
            std::vector<std::string> parts;
            std::string tmp = ps;
            while ((pos = tmp.find(':')) != std::string::npos) {
                parts.push_back(tmp.substr(0, pos));
                tmp.erase(0, pos + 1);
            }
            parts.push_back(tmp);
            if (parts.size() < 2) continue;

            std::string type = parts[0];
            std::string pname = parts[1];

            
            std::string label_text = pname;
            std::wstring wlabel = to_wide(label_text);
            CreateWindowExW(0, L"STATIC", wlabel.c_str(),
                WS_CHILD | WS_VISIBLE | SS_RIGHT,
                8, y, 110, 22, hDlg, nullptr, g_hinst, nullptr);

            if (type == "bool" && parts.size() >= 3) {
                HWND hChk = CreateWindowExW(0, L"BUTTON", L"",
                    WS_CHILD | WS_VISIBLE | BS_AUTOCHECKBOX,
                    120, y, 22, 22, hDlg, nullptr, g_hinst, nullptr);
                Button_SetCheck(hChk, parts[2] == "true" ? BST_CHECKED : BST_UNCHECKED);
                ParamCtrl pc;
                pc.name = pname; pc.type = type; pc.hwnd = hChk; pc.ctrl_id = d->next_id++;
                SetWindowLongPtrW(hChk, GWLP_USERDATA, (LONG_PTR)d);
                d->ctrls.push_back(pc);
                y += 28;
            } else if (type == "num" && parts.size() >= 6) {
                double val = std::stod(parts[2]);
                double mn  = std::stod(parts[3]);
                double mx  = std::stod(parts[4]);
                double stp = std::stod(parts[5]);
                int tck_w = cw - 120 - 80 - 16;
                if (tck_w < 60) tck_w = 60;
                HWND hTb = CreateWindowExW(0, L"msctls_trackbar32", L"",
                    WS_CHILD | WS_VISIBLE | TBS_HORZ | TBS_NOTICKS,
                    120, y + 2, tck_w, 24, hDlg, nullptr, g_hinst, nullptr);
                int range_min = (int)(mn / stp);
                int range_max = (int)(mx / stp);
                int range_val = (int)(val / stp);
                SendMessageW(hTb, TBM_SETRANGE, TRUE, MAKELPARAM(range_min, range_max));
                SendMessageW(hTb, TBM_SETPOS, TRUE, range_val);

                char buf[32];
                snprintf(buf, sizeof(buf), "%.2f", val);
                HWND hVal = CreateWindowExW(0, L"STATIC", to_wide(buf).c_str(),
                    WS_CHILD | WS_VISIBLE | SS_CENTER,
                    120 + tck_w + 4, y, 72, 22, hDlg, nullptr, g_hinst, nullptr);

                ParamCtrl pc;
                pc.name = pname; pc.type = type; pc.hwnd = hTb; pc.ctrl_id = d->next_id++;
                pc.num_val = val; pc.num_min = mn; pc.num_max = mx; pc.num_step = stp;
                SetWindowLongPtrW(hTb, GWLP_USERDATA, (LONG_PTR)d);
                
                
                d->ctrls.push_back(pc);
                y += 30;
            } else if (type == "mode" && parts.size() >= 4) {
                std::string cur_val = parts[2];
                std::string opts = parts[3];
                HWND hCmb = CreateWindowExW(0, L"COMBOBOX", L"",
                    WS_CHILD | WS_VISIBLE | CBS_DROPDOWNLIST | WS_VSCROLL,
                    120, y, 180, 200, hDlg, nullptr, g_hinst, nullptr);
                int sel_idx = 0;
                std::vector<std::string> mode_vals;
                std::string opt_rem = opts;
                size_t cp;
                int idx = 0;
                while ((cp = opt_rem.find(',')) != std::string::npos) {
                    std::string m = opt_rem.substr(0, cp);
                    mode_vals.push_back(m);
                    SendMessageW(hCmb, CB_ADDSTRING, 0, (LPARAM)to_wide(m).c_str());
                    if (m == cur_val) sel_idx = idx;
                    opt_rem.erase(0, cp + 1);
                    idx++;
                }
                if (!opt_rem.empty()) {
                    mode_vals.push_back(opt_rem);
                    SendMessageW(hCmb, CB_ADDSTRING, 0, (LPARAM)to_wide(opt_rem).c_str());
                    if (opt_rem == cur_val) sel_idx = idx;
                }
                SendMessageW(hCmb, CB_SETCURSEL, sel_idx, 0);
                ParamCtrl pc;
                pc.name = pname; pc.type = type; pc.hwnd = hCmb; pc.ctrl_id = d->next_id++;
                pc.modes = mode_vals;
                SetWindowLongPtrW(hCmb, GWLP_USERDATA, (LONG_PTR)d);
                d->ctrls.push_back(pc);
                y += 28;
            } else if (type == "str" && parts.size() >= 3) {
                HWND hEdt = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", to_wide(parts[2]).c_str(),
                    WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL,
                    120, y, 180, 22, hDlg, nullptr, g_hinst, nullptr);
                ParamCtrl pc;
                pc.name = pname; pc.type = type; pc.hwnd = hEdt; pc.ctrl_id = d->next_id++;
                pc.str_val = parts[2];
                SetWindowLongPtrW(hEdt, GWLP_USERDATA, (LONG_PTR)d);
                d->ctrls.push_back(pc);
                y += 28;
            } else if (type == "action") {
                HWND hBtn = CreateWindowExW(0, L"BUTTON", L"Execute",
                    WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
                    120, y, 100, 24, hDlg, nullptr, g_hinst, nullptr);
                ParamCtrl pc;
                pc.name = pname; pc.type = type; pc.hwnd = hBtn; pc.ctrl_id = d->next_id++;
                SetWindowLongPtrW(hBtn, GWLP_USERDATA, (LONG_PTR)d);
                d->ctrls.push_back(pc);
                y += 28;
            } else if (type == "color" && parts.size() >= 3) {
                HWND hEdt = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", to_wide(parts[2]).c_str(),
                    WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL,
                    120, y, 100, 22, hDlg, nullptr, g_hinst, nullptr);
                ParamCtrl pc;
                pc.name = pname; pc.type = type; pc.hwnd = hEdt; pc.ctrl_id = d->next_id++;
                pc.str_val = parts[2];
                SetWindowLongPtrW(hEdt, GWLP_USERDATA, (LONG_PTR)d);
                d->ctrls.push_back(pc);
                y += 28;
            }
        }

        
        CreateWindowExW(0, L"BUTTON", L"Close",
            WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
            (cw - 80) / 2, y + 8, 80, 28, hDlg, (HMENU)1, g_hinst, nullptr);

        return 0;
    }

    case WM_COMMAND: {
        if (!data) break;
        int id = LOWORD(wParam);
        int code = HIWORD(wParam);

        if (id == 1) { 
            DestroyWindow(hDlg);
            return 0;
        }

        
        for (auto& pc : data->ctrls) {
            if (pc.ctrl_id != id) continue;

            if (pc.type == "bool" && code == BN_CLICKED) {
                bool on = Button_GetCheck(pc.hwnd) == BST_CHECKED;
                notify_java_set_param(data->mod_name, pc.name, on ? "true" : "false");
            } else if (pc.type == "num" && (code == EN_CHANGE || code == 0)) {
                
            } else if (pc.type == "mode" && code == CBN_SELCHANGE) {
                int idx = (int)SendMessageW(pc.hwnd, CB_GETCURSEL, 0, 0);
                if (idx >= 0 && idx < (int)pc.modes.size()) {
                    notify_java_set_param(data->mod_name, pc.name, pc.modes[idx]);
                }
            } else if (pc.type == "str" && code == EN_CHANGE) {
                int len = (int)SendMessageW(pc.hwnd, WM_GETTEXTLENGTH, 0, 0);
                std::wstring wbuf(len + 1, L'\0');
                SendMessageW(pc.hwnd, WM_GETTEXT, len + 1, (LPARAM)&wbuf[0]);
                notify_java_set_param(data->mod_name, pc.name, to_utf8(wbuf));
            } else if (pc.type == "action" && code == BN_CLICKED) {
                notify_java_set_param(data->mod_name, pc.name, "trigger");
            } else if (pc.type == "color" && code == EN_CHANGE) {
                int len = (int)SendMessageW(pc.hwnd, WM_GETTEXTLENGTH, 0, 0);
                std::wstring wbuf(len + 1, L'\0');
                SendMessageW(pc.hwnd, WM_GETTEXT, len + 1, (LPARAM)&wbuf[0]);
                notify_java_set_param(data->mod_name, pc.name, to_utf8(wbuf));
            }
            break;
        }
        return 0;
    }

    case WM_HSCROLL: {
        if (!data) break;
        HWND hTb = (HWND)lParam;
        for (auto& pc : data->ctrls) {
            if (pc.type != "num" || pc.hwnd != hTb) continue;
            int pos = (int)SendMessageW(hTb, TBM_GETPOS, 0, 0);
            double val = pc.num_min + pos * pc.num_step;
            char buf[32];
            snprintf(buf, sizeof(buf), "%.2f", val);
            notify_java_set_param(data->mod_name, pc.name, buf);
            return 0;
        }
        return 0;
    }

    case WM_CTLCOLORSTATIC:
    case WM_CTLCOLORBTN:
    case WM_CTLCOLOREDIT: {
        HDC hdc = (HDC)wParam;
        SetBkColor(hdc, CLR_DLG_BG);
        SetTextColor(hdc, CLR_TEXT);
        static HBRUSH br = nullptr;
        if (!br) br = CreateSolidBrush(CLR_DLG_BG);
        return (LRESULT)br;
    }

    case WM_CLOSE:
        DestroyWindow(hDlg);
        return 0;

    case WM_DESTROY: {
        if (data) {
            delete data;
            SetWindowLongPtrW(hDlg, GWLP_USERDATA, 0);
        }
        EnableWindow(g_main_wnd, TRUE);
        SetForegroundWindow(g_main_wnd);
        return 0;
    }
    }

    return DefWindowProcW(hDlg, msg, wParam, lParam);
}

static void open_param_dialog(const std::string& mod_name) {
    std::string params = notify_java_get_params(mod_name);
    if (params.empty()) return;

    EnableWindow(g_main_wnd, FALSE);

    std::string spec = mod_name + "\n" + params;
    std::wstring wspec = to_wide(spec);

    int dlg_cx = 320;
    int dlg_cy = 300;

    HWND hDlg = CreateWindowExW(WS_EX_DLGMODALFRAME, L"RavexParamDialog",
        to_wide(mod_name).c_str(),
        WS_CAPTION | WS_SYSMENU | WS_THICKFRAME,
        CW_USEDEFAULT, CW_USEDEFAULT, dlg_cx, dlg_cy,
        g_main_wnd, nullptr, g_hinst, (LPVOID)wspec.c_str());

    if (hDlg) {
        
        RECT pr, dr;
        GetWindowRect(g_main_wnd, &pr);
        GetWindowRect(hDlg, &dr);
        int dw = dr.right - dr.left, dh = dr.bottom - dr.top;
        int px = pr.left + (pr.right - pr.left - dw) / 2;
        int py = pr.top + (pr.bottom - pr.top - dh) / 2;
        SetWindowPos(hDlg, nullptr, px, py, 0, 0, SWP_NOSIZE | SWP_NOZORDER);
        ShowWindow(hDlg, SW_SHOW);
    }
}



static void paint_main(HWND hWnd) {
    PAINTSTRUCT ps;
    HDC dc = BeginPaint(hWnd, &ps);

    HDC memDC = CreateCompatibleDC(dc);
    HBITMAP bmp = CreateCompatibleBitmap(dc, g_cx, g_cy);
    HGDIOBJ old_bmp = SelectObject(memDC, bmp);

    
    fill_rect(memDC, 0, 0, g_cx, g_cy, CLR_BG);

    
    fill_rect(memDC, 0, 0, g_cx, TITLE_H, CLR_BG);
    draw_text(memDC, 0, 0, g_cx, TITLE_H, "RaveX Desktop Dashboard", CLR_TITLE, g_font_bold, DT_CENTER);

    
    HPEN sep_pen = CreatePen(PS_SOLID, 1, CLR_BORDER);
    HGDIOBJ old_pen = SelectObject(memDC, sep_pen);
    MoveToEx(memDC, 8, TITLE_H, nullptr);
    LineTo(memDC, g_cx - 8, TITLE_H);
    SelectObject(memDC, old_pen);
    DeleteObject(sep_pen);

    
    int list_top = TITLE_H + 4;
    int list_h  = g_cy - list_top;
    int vis_rows = list_h / ROW_H;
    int start_row = g_scroll_y;

    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_total_rows = (int)g_modules.size();
    }

    int max_scroll = (std::max)(0, g_total_rows - vis_rows);

    for (int i = 0; i < vis_rows + 1 && start_row + i < g_total_rows; i++) {
        int row_idx = start_row + i;
        int ry = list_top + i * ROW_H;

        bool hovered = (row_idx == g_hover_row);
        COLORREF bg = hovered ? CLR_ROW_HOVER : CLR_ROW;
        fill_rect(memDC, 4, ry, g_cx - 8, ROW_H - 2, bg);

        
        HPEN rp = CreatePen(PS_SOLID, 1, CLR_BORDER);
        HGDIOBJ old_rp = SelectObject(memDC, rp);
        HBRUSH old_rb = (HBRUSH)SelectObject(memDC, GetStockObject(NULL_BRUSH));
        Rectangle(memDC, 4, ry, g_cx - 4, ry + ROW_H - 2);
        SelectObject(memDC, old_rp);
        SelectObject(memDC, old_rb);
        DeleteObject(rp);

        
        std::string mod_name;
        bool mod_enabled = false;
        {
            std::lock_guard<std::mutex> lock(g_mutex);
            if (row_idx < (int)g_modules.size()) {
                mod_name = g_modules[row_idx].name;
                auto it = g_enabled.find(mod_name);
                mod_enabled = (it != g_enabled.end()) ? it->second : g_modules[row_idx].enabled;
            }
        }
        if (mod_name.empty()) continue;

        int name_w = g_cx - 8 - PAD - GEAR_W - 4 - TOG_W - PAD;
        draw_text(memDC, 4 + PAD, ry, name_w, ROW_H - 2, mod_name, CLR_TEXT, g_font, DT_LEFT);

        
        int gx = 4 + PAD + name_w + 4;
        bool gear_hit = hovered && g_hover_gear && row_idx == g_hover_row;
        draw_text_w(memDC, gx, ry, GEAR_W, ROW_H - 2, L"\u2699", gear_hit ? CLR_GEAR_HVR : CLR_GEAR, g_font, DT_CENTER);

        
        int tx = gx + GEAR_W + 4;
        bool tog_state = mod_enabled;
        int tog_w = TOG_W;
        int tog_h = 24;
        int tog_x = tx;
        int tog_y = ry + (ROW_H - 2 - tog_h) / 2;
        draw_rounded_rect(memDC, tog_x, tog_y, tog_w, tog_h, tog_state ? CLR_ON : CLR_OFF, tog_state ? CLR_ON : CLR_BORDER);
        draw_text(memDC, tog_x, tog_y, tog_w, tog_h, tog_state ? "ON" : "OFF", CLR_BTN_TXT, g_font, DT_CENTER);
    }

    BitBlt(dc, 0, 0, g_cx, g_cy, memDC, 0, 0, SRCCOPY);

    SelectObject(memDC, old_bmp);
    DeleteObject(bmp);
    DeleteDC(memDC);

    EndPaint(hWnd, &ps);
}



static LRESULT CALLBACK MainWndProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
    case WM_CREATE:
        g_main_wnd = hWnd;
        break;

    case WM_SIZE: {
        RECT rc;
        GetClientRect(hWnd, &rc);
        g_cx = rc.right - rc.left;
        g_cy = rc.bottom - rc.top;
        int vis_rows = (g_cy - TITLE_H - 4) / ROW_H;
        g_scroll_y = (std::min)(g_scroll_y, (std::max)(0, g_total_rows - vis_rows));
        SCROLLINFO si = {sizeof(si), SIF_RANGE | SIF_PAGE | SIF_POS, 0, (UINT)(std::max)(0, g_total_rows), (UINT)vis_rows, g_scroll_y, 0};
        SetScrollInfo(hWnd, SB_VERT, &si, TRUE);
        InvalidateRect(hWnd, nullptr, FALSE);
        break;
    }

    case WM_PAINT:
        paint_main(hWnd);
        break;

    case WM_ERASEBKGND:
        return 1;

    case WM_VSCROLL: {
        int vis_rows = (g_cy - TITLE_H - 4) / ROW_H;
        int max_scroll = (std::max)(0, g_total_rows - vis_rows);
        int new_pos = g_scroll_y;
        switch (LOWORD(wParam)) {
        case SB_LINEUP:        new_pos -= 1; break;
        case SB_LINEDOWN:      new_pos += 1; break;
        case SB_PAGEUP:        new_pos -= vis_rows; break;
        case SB_PAGEDOWN:      new_pos += vis_rows; break;
        case SB_THUMBTRACK:
        case SB_THUMBPOSITION: new_pos = HIWORD(wParam); break;
        case SB_TOP:           new_pos = 0; break;
        case SB_BOTTOM:        new_pos = max_scroll; break;
        }
        new_pos = (std::max)(0, (std::min)(max_scroll, new_pos));
        if (new_pos != g_scroll_y) {
            g_scroll_y = new_pos;
            SCROLLINFO si = {sizeof(si), SIF_POS, 0, 0, 0, g_scroll_y, 0};
            SetScrollInfo(hWnd, SB_VERT, &si, TRUE);
            InvalidateRect(hWnd, nullptr, FALSE);
        }
        break;
    }

    case WM_MOUSEWHEEL: {
        int delta = GET_WHEEL_DELTA_WPARAM(wParam);
        int vis_rows = (g_cy - TITLE_H - 4) / ROW_H;
        int max_scroll = (std::max)(0, g_total_rows - vis_rows);
        int new_pos = g_scroll_y - delta / WHEEL_DELTA * 3;
        new_pos = (std::max)(0, (std::min)(max_scroll, new_pos));
        if (new_pos != g_scroll_y) {
            g_scroll_y = new_pos;
            SCROLLINFO si = {sizeof(si), SIF_POS, 0, 0, 0, g_scroll_y, 0};
            SetScrollInfo(hWnd, SB_VERT, &si, TRUE);
            InvalidateRect(hWnd, nullptr, FALSE);
        }
        break;
    }

    case WM_MOUSEMOVE: {
        int x = LOWORD(lParam);
        int y = HIWORD(lParam);
        int list_top = TITLE_H + 4;
        if (y > list_top) {
            int row = (y - list_top) / ROW_H + g_scroll_y;
            int col_x = x;
            bool was_hover_row = (g_hover_row == row);
            bool was_gear_state = g_hover_gear;

            int name_w = g_cx - 8 - PAD - GEAR_W - 4 - TOG_W - PAD;
            int gx = 4 + PAD + (std::max)(0, name_w) + 4;

            g_hover_row = row;
            g_hover_gear = (col_x >= gx && col_x < gx + GEAR_W);

            if (was_hover_row != (row == g_hover_row) || was_gear_state != g_hover_gear) {
                InvalidateRect(hWnd, nullptr, FALSE);
            }
        } else {
            if (g_hover_row != -1) {
                g_hover_row = -1;
                InvalidateRect(hWnd, nullptr, FALSE);
            }
        }
        break;
    }

    case WM_LBUTTONDOWN: {
        int x = LOWORD(lParam);
        int y = HIWORD(lParam);
        int list_top = TITLE_H + 4;
        if (y <= list_top) break;

        int row = (y - list_top) / ROW_H + g_scroll_y;
        int col_x = x;

        std::string mod_name;
        {
            std::lock_guard<std::mutex> lock(g_mutex);
            if (row < 0 || row >= (int)g_modules.size()) break;
            mod_name = g_modules[row].name;
        }

        int name_w = g_cx - 8 - PAD - GEAR_W - 4 - TOG_W - PAD;
        int gx = 4 + PAD + (std::max)(0, name_w) + 4;
        int tx = gx + GEAR_W + 4;

        if (col_x >= gx && col_x < gx + GEAR_W) {
            open_param_dialog(mod_name);
        } else if (col_x >= tx && col_x < tx + TOG_W) {
            
            {
                std::lock_guard<std::mutex> lock(g_mutex);
                g_enabled[mod_name] = !g_enabled[mod_name];
            }
            InvalidateRect(hWnd, nullptr, FALSE);
            notify_java_toggle(mod_name);
        }
        break;
    }

    case WM_UPDATE_TOGGLE: {
        auto data = (UpdateToggleData*)lParam;
        if (data) {
            {
                std::lock_guard<std::mutex> lock(g_mutex);
                g_enabled[data->name] = data->enabled;
            }
            InvalidateRect(hWnd, nullptr, FALSE);
            delete data;
        }
        return 0;
    }

    case WM_UPDATE_ALL: {
        auto modules = (std::vector<ravex::ModuleGuiData>*)lParam;
        if (modules) {
            {
                std::lock_guard<std::mutex> lock(g_mutex);
                g_modules = *modules;
            }
            delete modules;
        }
        int vis_rows = (g_cy - TITLE_H - 4) / ROW_H;
        g_scroll_y = (std::min)(g_scroll_y, (std::max)(0, g_total_rows - vis_rows));
        SCROLLINFO si = {sizeof(si), SIF_RANGE | SIF_PAGE | SIF_POS, 0, (UINT)(std::max)(0, g_total_rows), (UINT)vis_rows, g_scroll_y, 0};
        SetScrollInfo(hWnd, SB_VERT, &si, TRUE);
        InvalidateRect(hWnd, nullptr, FALSE);
        return 0;
    }

    case WM_DESTROY: {
        g_main_wnd = nullptr;
        notify_java_close();
        PostQuitMessage(0);
        return 0;
    }
    }

    return DefWindowProcW(hWnd, msg, wParam, lParam);
}



static void window_thread() {
    CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);

    INITCOMMONCONTROLSEX icex;
    icex.dwSize = sizeof(icex);
    icex.dwICC = ICC_BAR_CLASSES | ICC_STANDARD_CLASSES;
    InitCommonControlsEx(&icex);

    g_hinst = GetModuleHandleW(nullptr);

    
    LOGFONTW lf = {0};
    lf.lfHeight = 15;
    lf.lfWeight = FW_NORMAL;
    lf.lfQuality = CLEARTYPE_QUALITY;
    wcscpy_s(lf.lfFaceName, L"Segoe UI");
    g_font = CreateFontIndirectW(&lf);

    lf.lfHeight = 17;
    lf.lfWeight = FW_BOLD;
    g_font_bold = CreateFontIndirectW(&lf);

    
    WNDCLASSEXW wc = {sizeof(wc)};
    wc.style = CS_HREDRAW | CS_VREDRAW | CS_DBLCLKS;
    wc.lpfnWndProc = MainWndProc;
    wc.hInstance = g_hinst;
    wc.hCursor = LoadCursor(nullptr, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)CreateSolidBrush(CLR_BG);
    wc.lpszClassName = L"RavexDesktopGuiMain";
    RegisterClassExW(&wc);

    
    WNDCLASSEXW dc = {sizeof(dc)};
    dc.style = CS_HREDRAW | CS_VREDRAW;
    dc.lpfnWndProc = ParamDlgProc;
    dc.hInstance = g_hinst;
    dc.hCursor = LoadCursor(nullptr, IDC_ARROW);
    dc.hbrBackground = (HBRUSH)CreateSolidBrush(CLR_DLG_BG);
    dc.lpszClassName = L"RavexParamDialog";
    RegisterClassExW(&dc);

    
    HWND hWnd = CreateWindowExW(WS_EX_WINDOWEDGE, L"RavexDesktopGuiMain", L"RaveX Desktop Dashboard",
        WS_OVERLAPPEDWINDOW | WS_VSCROLL,
        CW_USEDEFAULT, CW_USEDEFAULT, WIN_CX, WIN_CY,
        nullptr, nullptr, g_hinst, nullptr);

    if (!hWnd) {
        if (g_font) DeleteObject(g_font);
        if (g_font_bold) DeleteObject(g_font_bold);
        CoUninitialize();
        return;
    }

    ShowWindow(hWnd, SW_SHOW);
    UpdateWindow(hWnd);

    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    if (g_font) DeleteObject(g_font);
    if (g_font_bold) DeleteObject(g_font_bold);
    g_font = nullptr;
    g_font_bold = nullptr;
    g_main_wnd = nullptr;

    CoUninitialize();
}



void ravex::start_gui(const std::vector<ravex::ModuleGuiData>& modules) {
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_modules = modules;
        g_enabled.clear();
        for (const auto& m : modules) g_enabled[m.name] = m.enabled;
    }
    std::thread(window_thread).detach();
}

void ravex::update_gui_state(const std::string& name, bool enabled) {
    if (!g_main_wnd) return;
    auto data = new UpdateToggleData{name, enabled};
    PostMessageW(g_main_wnd, WM_UPDATE_TOGGLE, 0, (LPARAM)data);
}

void ravex::stop_gui() {
    HWND hw = g_main_wnd;
    if (hw) {
        PostMessageW(hw, WM_CLOSE, 0, 0);
    }
}



extern "C" {

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_8;
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_openDesktopGui(JNIEnv* env, jclass cls,
    jobjectArray names, jbooleanArray states)
{
    if (g_desktopgui_class) env->DeleteGlobalRef(g_desktopgui_class);
    g_desktopgui_class = (jclass)env->NewGlobalRef(cls);

    std::vector<ravex::ModuleGuiData> modules;
    jsize len = env->GetArrayLength(names);
    jboolean* sbuf = env->GetBooleanArrayElements(states, nullptr);
    for (jsize i = 0; i < len; i++) {
        jstring jn = (jstring)env->GetObjectArrayElement(names, i);
        const char* c = env->GetStringUTFChars(jn, nullptr);
        modules.push_back({std::string(c), sbuf[i] == JNI_TRUE});
        env->ReleaseStringUTFChars(jn, c);
        env->DeleteLocalRef(jn);
    }
    env->ReleaseBooleanArrayElements(states, sbuf, JNI_ABORT);

    ravex::start_gui(modules);
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_updateModuleState(JNIEnv* env, jclass cls,
    jstring name, jboolean enabled)
{
    const char* c = env->GetStringUTFChars(name, nullptr);
    ravex::update_gui_state(std::string(c), enabled == JNI_TRUE);
    env->ReleaseStringUTFChars(name, c);
}

JNIEXPORT void JNICALL
Java_ravex_modules_client_DesktopGui_closeDesktopGui(JNIEnv* env, jclass cls) {
    ravex::stop_gui();
}

}
