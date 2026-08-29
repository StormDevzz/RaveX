#include "ui/include/crash_dialog.hpp"
#include "core/include/lang.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "ui/include/glow.hpp"
#include <windows.h>
#include <shellapi.h>
#include <commctrl.h>
#include <gdiplus.h>
#include <string>
namespace ravex::ui {
namespace {
constexpr COLORREF kBg = RGB(28, 28, 30);
constexpr COLORREF kPanel = RGB(40, 40, 42);
HFONT makeFontCrash(int h = -13, int w = FW_NORMAL) {
    HFONT f = CreateFontW(h, 0, 0, 0, w, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Manrope");
    if (!f) f = CreateFontW(h, 0, 0, 0, w, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    if (!f) f = CreateFontW(h, 0, 0, 0, w, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    return f;
}
struct CrashData {
    bool closed = false;
    int exitCode = 0;
    std::wstring crashPath;
    std::wstring logPath;
    std::string reason;
    HWND hReason = nullptr;
    HWND hCrashPath = nullptr;
    HWND hLogPath = nullptr;
    HWND hOpenCrash = nullptr;
    HWND hOpenLog = nullptr;
    HWND hClose = nullptr;
    HFONT font = nullptr;
    HFONT smallFont = nullptr;
    HBRUSH bgBrush = nullptr;
    GlowData glow;
};
LRESULT CALLBACK CrashProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    CrashData* d = reinterpret_cast<CrashData*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
    switch (msg) {
        case WM_CREATE: {
            CREATESTRUCTW* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            d = reinterpret_cast<CrashData*>(cs->lpCreateParams);
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(d));
            d->font = makeFontCrash(-13, FW_NORMAL);
            d->smallFont = makeFontCrash(-11, FW_NORMAL);
            d->bgBrush = CreateSolidBrush(kBg);
            HINSTANCE inst = cs->hInstance;
            auto tr=[&](const char* k,const char* fb){const char* v=lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
            CreateWindowExW(0, L"STATIC", tr("crash_title","Game crashed").c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 16, 460, 22, hwnd, nullptr, inst, nullptr);
            std::wstring codeTxt = tr("crash_exit_code","Exit code") + L": " + std::to_wstring(d->exitCode);
            CreateWindowExW(0, L"STATIC", codeTxt.c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 42, 460, 16, hwnd, nullptr, inst, nullptr);
            std::wstring reasonW = fromUtf8(d->reason);
            if (reasonW.size() > 800) reasonW = reasonW.substr(0, 800) + L"...";
            d->hReason = CreateWindowExW(0, L"EDIT", reasonW.c_str(), WS_CHILD | WS_VISIBLE | WS_BORDER | ES_MULTILINE | ES_AUTOVSCROLL | ES_READONLY | WS_VSCROLL, 20, 62, 460, 140, hwnd, nullptr, inst, nullptr);
            std::wstring crashLbl = tr("crash_report_path","Crash report:") + L" " + (d->crashPath.empty() ? tr("not_found","Not found") : d->crashPath);
            d->hCrashPath = CreateWindowExW(0, L"STATIC", crashLbl.c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 210, 460, 36, hwnd, nullptr, inst, nullptr);
            std::wstring logLbl = tr("log_path","Log:") + L" " + (d->logPath.empty() ? tr("not_found","Not found") : d->logPath);
            d->hLogPath = CreateWindowExW(0, L"STATIC", logLbl.c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 20, 250, 460, 36, hwnd, nullptr, inst, nullptr);
            d->hOpenCrash = CreateWindowExW(0, L"BUTTON", tr("open_crash_report","Open crash report").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW, 20, 300, 140, 28, hwnd, reinterpret_cast<HMENU>(1001), inst, nullptr);
            d->hOpenLog = CreateWindowExW(0, L"BUTTON", tr("open_log","Open log").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW, 170, 300, 100, 28, hwnd, reinterpret_cast<HMENU>(1002), inst, nullptr);
            d->hClose = CreateWindowExW(0, L"BUTTON", tr("close","Close").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW, 340, 300, 140, 28, hwnd, reinterpret_cast<HMENU>(IDCANCEL), inst, nullptr);
            for (HWND c : {d->hReason, d->hOpenCrash, d->hOpenLog, d->hClose}) SendMessageW(c, WM_SETFONT, reinterpret_cast<WPARAM>(d->font), TRUE);
            for (HWND c : {d->hCrashPath, d->hLogPath}) SendMessageW(c, WM_SETFONT, reinterpret_cast<WPARAM>(d->smallFont), TRUE);
            EnableWindow(d->hOpenCrash, d->crashPath.empty() ? FALSE : TRUE);
            EnableWindow(d->hOpenLog, d->logPath.empty() ? FALSE : TRUE);
            glowCreate(hwnd, &d->glow);
            glowSetButtons(&d->glow, {d->hOpenCrash, d->hOpenLog, d->hClose});
            SetTimer(hwnd, 99, 16, nullptr);
            return 0;
        }
        case WM_MOUSEMOVE: glowUpdate(&d->glow); return 0;
        case WM_TIMER: if (wParam==99) glowUpdate(&d->glow); return 0;
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds=(DRAWITEMSTRUCT*)lParam;
            if (ds->CtlID==1001 || ds->CtlID==1002 || ds->CtlID==IDCANCEL) {
                HDC dc=ds->hDC; RECT rc=ds->rcItem;
                bool isClose=(ds->CtlID==IDCANCEL);
                COLORREF bg=isClose?RGB(45,45,47):RGB(90,140,255);
                if(ds->itemState & ODS_DISABLED) bg=RGB(60,60,60);
                HBRUSH bf=CreateSolidBrush(bg); FillRect(dc,&rc,bf); DeleteObject(bf);
                Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                Gdiplus::Color gc(GetRValue(bg),GetGValue(bg),GetBValue(bg)); Gdiplus::SolidBrush br(gc);
                Gdiplus::GraphicsPath path; path.AddArc(rc.left,rc.top,8,8,180,90); path.AddArc(rc.right-8,rc.top,8,8,270,90); path.AddArc(rc.right-8,rc.bottom-8,8,8,0,90); path.AddArc(rc.left,rc.bottom-8,8,8,90,90); path.CloseFigure();
                g.FillPath(&br,&path);
                wchar_t txt[64]; GetWindowTextW(ds->hwndItem,txt,64);
                Gdiplus::SolidBrush tbr(Gdiplus::Color(255,255,255));
                Gdiplus::Font f(dc,d->font);
                Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentCenter); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                Gdiplus::RectF rf((Gdiplus::REAL)rc.left,(Gdiplus::REAL)rc.top,(Gdiplus::REAL)(rc.right-rc.left),(Gdiplus::REAL)(rc.bottom-rc.top));
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                g.DrawString(txt,-1,&f,rf,&fmt,&tbr);
                return TRUE;
            }
            return FALSE;
        }
        case WM_CTLCOLORSTATIC: {
            HDC dc=reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, kBg); SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(d->bgBrush ? d->bgBrush : GetStockObject(BLACK_BRUSH));
        }
        case WM_CTLCOLOREDIT: {
            HDC dc=reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, kPanel); SetTextColor(dc, RGB(255,255,255));
            static HBRUSH pBr=nullptr; if(!pBr) pBr=CreateSolidBrush(kPanel);
            return reinterpret_cast<LRESULT>(pBr);
        }
        case WM_ERASEBKGND: {
            HDC dc=reinterpret_cast<HDC>(wParam); RECT rc; GetClientRect(hwnd,&rc); HBRUSH b=CreateSolidBrush(kBg); FillRect(dc,&rc,b); DeleteObject(b); return 1;
        }
        case WM_COMMAND:
            if (LOWORD(wParam)==1001) { if(!d->crashPath.empty()) ShellExecuteW(hwnd, L"open", d->crashPath.c_str(), nullptr, nullptr, SW_SHOWNORMAL); return 0; }
            if (LOWORD(wParam)==1002) { if(!d->logPath.empty()) ShellExecuteW(hwnd, L"open", d->logPath.c_str(), nullptr, nullptr, SW_SHOWNORMAL); return 0; }
            if (LOWORD(wParam)==IDCANCEL) { d->closed=true; DestroyWindow(hwnd); return 0; }
            return 0;
        case WM_CLOSE: d->closed=true; DestroyWindow(hwnd); return 0;
        case WM_DESTROY: KillTimer(hwnd,99); glowDestroy(&d->glow); if(d) { DeleteObject(d->font); DeleteObject(d->smallFont); if(d->bgBrush) DeleteObject(d->bgBrush); d->closed=true; } return 0;
        default: return DefWindowProcW(hwnd,msg,wParam,lParam);
    }
}
}
bool showCrashDialog(HWND parent, int exitCode, const std::wstring& crashPath, const std::wstring& logPath, const std::string& reason) {
    HINSTANCE inst = reinterpret_cast<HINSTANCE>(GetWindowLongPtrW(parent, GWLP_HINSTANCE));
    if (!inst) inst = GetModuleHandleW(nullptr);
    static bool reg=false;
    if(!reg){ WNDCLASSEXW wc{}; wc.cbSize=sizeof(wc); wc.lpfnWndProc=CrashProc; wc.hInstance=inst; wc.hIcon=LoadIconW(inst, MAKEINTRESOURCEW(101)); if(!wc.hIcon) wc.hIcon=LoadIconW(nullptr, MAKEINTRESOURCEW(32512)); wc.hIconSm=wc.hIcon; wc.hCursor=LoadCursorW(nullptr, MAKEINTRESOURCEW(32512)); wc.lpszClassName=L"KickxCrashDlg"; RegisterClassExW(&wc); reg=true; }
    CrashData d; d.exitCode=exitCode; d.crashPath=crashPath; d.logPath=logPath; d.reason=reason;
    RECT pr{}; GetWindowRect(parent,&pr); int pw=pr.right-pr.left, ph=pr.bottom-pr.top;
    RECT cr{0,0,500,360}; AdjustWindowRectEx(&cr, WS_POPUP|WS_CAPTION|WS_SYSMENU, FALSE, 0);
    int dw=cr.right-cr.left, dh=cr.bottom-cr.top;
    int x=pr.left+(pw-dw)/2, y=pr.top+(ph-dh)/2;
    HMONITOR mon=MonitorFromWindow(parent, MONITOR_DEFAULTTONEAREST); MONITORINFO mi{}; mi.cbSize=sizeof(mi); GetMonitorInfoW(mon,&mi); RECT wr=mi.rcWork;
    if(x<wr.left) x=wr.left+16; if(y<wr.top) y=wr.top+16; if(x+dw>wr.right) x=wr.right-dw-16; if(y+dh>wr.bottom) y=wr.bottom-dh-16;
    auto trTitle=[&](const char* k,const char* fb){const char* v=lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
    HWND hwnd=CreateWindowExW(0, L"KickxCrashDlg", trTitle("crash_title","Game crashed").c_str(), WS_POPUP|WS_CAPTION|WS_SYSMENU, x,y,dw,dh,parent,nullptr,inst,&d);
    if(!hwnd) return false;
    ShowWindow(hwnd, SW_SHOW); UpdateWindow(hwnd);
    EnableWindow(parent,FALSE);
    MSG msg; while(!d.closed){ while(PeekMessageW(&msg,nullptr,0,0,PM_REMOVE)){ if(IsDialogMessageW(hwnd,&msg)) continue; TranslateMessage(&msg); DispatchMessageW(&msg);} Sleep(10); }
    EnableWindow(parent,TRUE); SetForegroundWindow(parent);
    return true;
}
}
