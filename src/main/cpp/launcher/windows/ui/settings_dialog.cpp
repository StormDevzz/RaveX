#include "ui/include/settings_dialog.hpp"
#include "core/include/config.hpp"
#include "core/include/lang.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "ui/include/glow.hpp"
#include "ui/include/color_dialog.hpp"
#include "build_info.h"
#include <cstring>
#include <windows.h>
#include <dwmapi.h>
#include <commctrl.h>
#include <uxtheme.h>
#include <gdiplus.h>
#include <string>
#include <vector>
namespace ravex::ui {
namespace {
constexpr int IDC_CHK_UPDATE = 2101;
constexpr int IDC_CHK_SNAP = 2102;
constexpr int IDC_CHK_BETA = 2103;
constexpr int IDC_CHK_ALPHA = 2104;
constexpr int IDC_LANG_COMBO = 2105;
constexpr int IDC_THEME_COMBO = 2106;
constexpr int IDC_BG_BTN = 2107;
constexpr int IDC_PANEL_BTN = 2108;
constexpr int IDC_TEXT_BTN = 2109;
constexpr int IDC_ACCENT_BTN = 2110;
constexpr int IDC_PREVIEW = 2111;
constexpr int IDC_BUTTON_BTN = 2112;
constexpr int IDC_CHK_LOGS = 2113;
constexpr int IDC_SYS_INFO = 2114;
constexpr int IDC_CHK_GLOW = 2115;
constexpr int IDC_GLOW_BTN = 2116;
struct SettingsData {
    bool closed = false;
    bool ok = false;
    LauncherConfig cfg;
    ThemeColors cur;
    std::string selected;
    std::vector<HBITMAP> flagBmps;
    HWND hwnd = nullptr;
    HWND hUpdate = nullptr;
    HWND hSnap = nullptr;
    HWND hBeta = nullptr;
    HWND hAlpha = nullptr;
    bool cUpdate = false;
    bool cSnap = false;
    bool cBeta = false;
    bool cAlpha = false;
    HWND hLogs = nullptr;
    bool cLogs = false;
    HWND hGlowChk = nullptr;
    bool cGlow = false;
    HWND hGlowBtn = nullptr;
    HWND hLangCombo = nullptr;
    HWND hThemeCombo = nullptr;
    HWND hBgBtn = nullptr;
    HWND hPanelBtn = nullptr;
    HWND hTextBtn = nullptr;
    HWND hAccentBtn = nullptr;
    HWND hButtonBtn = nullptr;
    HWND hPreview = nullptr;
    HWND hSysHeader = nullptr;
    HWND hSysInfo = nullptr;
    HWND hBuild = nullptr;
    HFONT font = nullptr;
    HFONT smallFont = nullptr;
    HBRUSH bgBrush = nullptr;
    ULONG_PTR gdiToken = 0;
    GlowData glow;
    HWND hSave = nullptr;
    HWND hCancel = nullptr;
};
std::wstring getSystemInfoTextForLang(const char* code) {
    auto tr=[&](const char* k,const char* fb){
        const char* v=langFor(k, code);
        if(!v||strcmp(v,k)==0) v=fb;
        return fromUtf8(v);
    };
    OSVERSIONINFOEXW os{}; os.dwOSVersionInfoSize=sizeof(os);
    typedef LONG (WINAPI* RtlGetVersion_t)(OSVERSIONINFOEXW*);
    if (auto fn=(RtlGetVersion_t)GetProcAddress(GetModuleHandleW(L"ntdll.dll"), "RtlGetVersion")) fn(&os);
    else GetVersionExW((OSVERSIONINFOW*)&os);
    bool is11 = (os.dwMajorVersion>=10 && os.dwBuildNumber>=22000);
    std::wstring name = is11 ? L"Windows 11" : L"Windows 10";
    wchar_t ver[128]; swprintf(ver,128,L"%s Build %lu", name.c_str(), os.dwBuildNumber);
    std::wstring out = ver;
    HKEY hk; wchar_t disp[128]={}; DWORD sz=sizeof(disp);
    if (RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",0,KEY_READ,&hk)==ERROR_SUCCESS){
        DWORD s=sizeof(disp); RegQueryValueExW(hk,L"DisplayVersion",nullptr,nullptr,(BYTE*)disp,&s);
        if(disp[0]) { out+= L" "; out+=disp; }
        RegCloseKey(hk);
    }
    out += is11 ? L" (Windows 11)" : L" (Windows 10)";
    out += L"\r\n";
    MEMORYSTATUSEX mem{}; mem.dwLength=sizeof(mem); GlobalMemoryStatusEx(&mem);
    std::wstring ramLb = tr("sysinfo_ram","RAM");
    wchar_t ram[96]; swprintf(ram,96,L"%s %llu GB / %llu GB (%lu%%)", ramLb.c_str(), mem.ullTotalPhys/(1024*1024*1024), mem.ullAvailPhys/(1024*1024*1024), mem.dwMemoryLoad);
    out+=ram;
    SYSTEM_INFO si{}; GetNativeSystemInfo(&si);
    const wchar_t* arch = (si.wProcessorArchitecture==PROCESSOR_ARCHITECTURE_AMD64)?L" x64":(si.wProcessorArchitecture==PROCESSOR_ARCHITECTURE_INTEL)?L" x86":L" ARM";
    out+=arch;
    out+= L" | CPU x" + std::to_wstring(si.dwNumberOfProcessors);
    wchar_t cpuName[256]={}; DWORD cnSz=sizeof(cpuName);
    if(RegOpenKeyExW(HKEY_LOCAL_MACHINE, L"Hardware\\Description\\System\\CentralProcessor\\0",0,KEY_READ,&hk)==ERROR_SUCCESS){
        RegQueryValueExW(hk,L"ProcessorNameString",nullptr,nullptr,(BYTE*)cpuName,&cnSz); RegCloseKey(hk);
    }
    if(cpuName[0]){ out+=L"\r\n"; out+=cpuName; }
    ULARGE_INTEGER freeBytesAvail, totalBytes;
    if(GetDiskFreeSpaceExW(L"C:\\",&freeBytesAvail,&totalBytes,nullptr)){
     std::wstring diskLb = tr("sysinfo_disk","Disk");
     std::wstring freeLb = tr("sysinfo_free","free");
     out+=L"\r\n" + diskLb + L": " + std::to_wstring(totalBytes.QuadPart/(1024*1024*1024)) + L" GB (" + std::to_wstring(freeBytesAvail.QuadPart/(1024*1024*1024)) + L" GB " + freeLb + L")";
    }
    DEVMODEW dm{}; dm.dmSize=sizeof(dm);
    if(EnumDisplaySettingsW(nullptr,ENUM_CURRENT_SETTINGS,&dm)){
     std::wstring dispLb = tr("sysinfo_display","Display");
     out+=L"\r\n" + dispLb + L": " + std::to_wstring(dm.dmPelsWidth) + L"x" + std::to_wstring(dm.dmPelsHeight) + L" " + std::to_wstring(dm.dmDisplayFrequency) + L"Hz";
    }
    return out;
}
std::wstring getSystemInfoText() { return getSystemInfoTextForLang(currentLanguage()); }
HFONT makeSettingsFont(int h = -13) {
    HFONT f = CreateFontW(h, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Manrope");
    if (!f) f = CreateFontW(h, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    if (!f) f = CreateFontW(h, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    return f;
}
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
std::wstring flagPathForCode(const std::string& code) {
    wchar_t exe[MAX_PATH]; GetModuleFileNameW(nullptr, exe, MAX_PATH);
    std::wstring exeDir = exe;
    size_t p = exeDir.find_last_of(L"\\/");
    if (p != std::wstring::npos) exeDir = exeDir.substr(0, p);
    std::wstring a = exeDir + L"\\flags\\" + fromUtf8(code) + L".png";
    if (fileExists(a)) return a;
    std::wstring b = joinPath(exeDir, L"..\\..\\src\\main\\cpp\\launcher\\windows\\flags\\" + fromUtf8(code) + L".png");
    if (fileExists(b)) return b;
    std::wstring c = L"C:\\Users\\user\\RaveX\\src\\main\\cpp\\launcher\\windows\\flags\\" + fromUtf8(code) + L".png";
    if (fileExists(c)) return c;
    return L"";
}
COLORREF pickColor(HWND parent, COLORREF init, int* alpha = nullptr) {
    CHOOSECOLORW cc{};
    static COLORREF custom[16] = {RGB(255,255,255),RGB(0,0,0),RGB(90,140,255),RGB(255,0,0),RGB(40,40,42),RGB(28,28,30)};
    cc.lStructSize = sizeof(cc);
    cc.hwndOwner = parent;
    COLORREF r = showColorDialog(parent, init, alpha);
    return r;
}
static COLORREF contrastText(COLORREF bg) {
    int r = GetRValue(bg), g = GetGValue(bg), b = GetBValue(bg);
    double lum = 0.299 * r + 0.587 * g + 0.114 * b;
    return lum > 140 ? RGB(20, 20, 20) : RGB(255,255,255);
}

void updateThemeState(SettingsData* d) {
    bool isCustom = d->selected == "custom";
    EnableWindow(d->hBgBtn, isCustom);
    EnableWindow(d->hPanelBtn, isCustom);
    EnableWindow(d->hTextBtn, isCustom);
    EnableWindow(d->hAccentBtn, isCustom);
    EnableWindow(d->hButtonBtn, isCustom);
    EnableWindow(d->hGlowChk, isCustom);
    EnableWindow(d->hGlowBtn, isCustom && d->cGlow);
    if (d->hGlowChk) InvalidateRect(d->hGlowChk, nullptr, TRUE);
    if (d->hGlowBtn) InvalidateRect(d->hGlowBtn, nullptr, TRUE);
    if (isCustom) {
        d->cur.bg = d->cfg.customBg;
        d->cur.panel = d->cfg.customPanel;
        d->cur.text = d->cfg.customText;
        d->cur.accent = d->cfg.customAccent;
        d->cur.buttonBg = d->cfg.customButton ? d->cfg.customButton : d->cfg.customAccent;
        d->cur.glow = d->cfg.customGlow ? d->cfg.customGlow : RGB(200, 225, 255);
        d->cur.glowEnabled = d->cGlow;
        d->cur.colorAlpha = d->cfg.customAlpha;
    } else {
        d->cur = getTheme(d->selected);
    }
    if (d->bgBrush) DeleteObject(d->bgBrush);
    d->bgBrush = CreateSolidBrush(d->cur.bg);
    if (d->hwnd) {
        applyWindowTheme(d->hwnd, d->cur);
        InvalidateRect(d->hwnd, nullptr, TRUE);
    }
    if (d->hPreview) InvalidateRect(d->hPreview, nullptr, TRUE);
    if (d->hBgBtn) InvalidateRect(d->hBgBtn, nullptr, TRUE);
    if (d->hPanelBtn) InvalidateRect(d->hPanelBtn, nullptr, TRUE);
    if (d->hTextBtn) InvalidateRect(d->hTextBtn, nullptr, TRUE);
    if (d->hAccentBtn) InvalidateRect(d->hAccentBtn, nullptr, TRUE);
    if (d->hButtonBtn) InvalidateRect(d->hButtonBtn, nullptr, TRUE);
}
void applyThemeChoice(SettingsData* d) {
    int sel = (int)SendMessageW(d->hThemeCombo, CB_GETCURSEL, 0, 0);
    if (sel == 0) d->selected = "dark";
    else if (sel == 1) d->selected = "light";
    else if (sel == 2) d->selected = "midnight";
    else if (sel == 3) d->selected = "ocean";
    else if (sel == 4) d->selected = "forest";
    else d->selected = "custom";
    updateThemeState(d);
}
LRESULT CALLBACK SettingsProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    SettingsData* data = reinterpret_cast<SettingsData*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
    switch (msg) {
        case WM_CREATE: {
            CREATESTRUCTW* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            data = reinterpret_cast<SettingsData*>(cs->lpCreateParams);
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(data));
            data->hwnd = hwnd;
            Gdiplus::GdiplusStartupInput si; Gdiplus::GdiplusStartup(&data->gdiToken, &si, nullptr);
            data->font = makeSettingsFont(-13);
            data->smallFont = makeSettingsFont(-11);
            data->cur = getThemeForConfig(data->cfg.theme, data->cfg.customBg, data->cfg.customPanel, data->cfg.customText, data->cfg.customAccent, 0, 255, data->cfg.customGlow, data->cfg.glowEnabled);
            data->bgBrush = CreateSolidBrush(data->cur.bg);
            data->selected = data->cfg.theme;
            HINSTANCE inst = cs->hInstance;
            auto tr=[&](const char* k,const char* fb){const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
            CreateWindowExW(0, L"STATIC", tr("settings","Settings").c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 16, 14, 400, 20, hwnd, nullptr, inst, nullptr);
            data->cUpdate = data->cfg.checkUpdatesOnStart;
            data->cSnap = data->cfg.showSnapshots;
            data->cBeta = data->cfg.showBeta;
            data->cAlpha = data->cfg.showAlpha;
            data->cLogs = data->cfg.saveLogs;
            data->cGlow = data->cfg.glowEnabled;
            data->hUpdate = CreateWindowExW(0, L"BUTTON", tr("check_update_on_start","Check for updates on start").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 44, 400, 22, hwnd, reinterpret_cast<HMENU>(IDC_CHK_UPDATE), inst, nullptr);
            data->hSnap = CreateWindowExW(0, L"BUTTON", tr("show_snapshots","Show snapshots").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 72, 400, 22, hwnd, reinterpret_cast<HMENU>(IDC_CHK_SNAP), inst, nullptr);
            data->hBeta = CreateWindowExW(0, L"BUTTON", tr("show_beta","Show beta versions").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 100, 400, 22, hwnd, reinterpret_cast<HMENU>(IDC_CHK_BETA), inst, nullptr);
            data->hAlpha = CreateWindowExW(0, L"BUTTON", tr("show_alpha","Show alpha versions").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 128, 400, 22, hwnd, reinterpret_cast<HMENU>(IDC_CHK_ALPHA), inst, nullptr);
            data->hLogs = CreateWindowExW(0, L"BUTTON", tr("save_logs","Save game logs").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 156, 400, 22, hwnd, reinterpret_cast<HMENU>(IDC_CHK_LOGS), inst, nullptr);
            CreateWindowExW(0, L"STATIC", tr("language","Language").c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 16, 192, 90, 20, hwnd, nullptr, inst, nullptr);
            data->hLangCombo = CreateWindowExW(0, L"COMBOBOX", nullptr, WS_CHILD | WS_VISIBLE | CBS_OWNERDRAWFIXED | CBS_DROPDOWNLIST | CBS_HASSTRINGS | WS_VSCROLL, 110, 190, 310, 260, hwnd, reinterpret_cast<HMENU>(IDC_LANG_COMBO), inst, nullptr);
            int langCount = ravex::langCount();
            int selLang = 0;
            for (int i = 0; i < langCount; ++i) {
                const char* code = ravex::langCodeByIndex(i);
                const char* name = ravex::langDisplayName(code);
                std::wstring disp = fromUtf8(name);
                int idx = (int)SendMessageW(data->hLangCombo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(disp.c_str()));
                SendMessageW(data->hLangCombo, CB_SETITEMDATA, idx, i);
                std::wstring fp = flagPathForCode(code);
                HBITMAP hb = fp.empty() ? nullptr : loadFlagBmp(fp);
                data->flagBmps.push_back(hb);
                if (data->cfg.language == code) selLang = idx;
            }
            SendMessageW(data->hLangCombo, CB_SETCURSEL, selLang, 0);
            CreateWindowExW(0, L"STATIC", tr("theme","Theme").c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 16, 224, 90, 20, hwnd, nullptr, inst, nullptr);
            data->hThemeCombo = CreateWindowExW(0, L"COMBOBOX", nullptr, WS_CHILD | WS_VISIBLE | CBS_DROPDOWNLIST | WS_VSCROLL, 110, 222, 310, 200, hwnd, reinterpret_cast<HMENU>(IDC_THEME_COMBO), inst, nullptr);
            SendMessageW(data->hThemeCombo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(tr("dark","Dark").c_str()));
            SendMessageW(data->hThemeCombo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(tr("light","Light").c_str()));
            SendMessageW(data->hThemeCombo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(tr("midnight","Midnight").c_str()));
            SendMessageW(data->hThemeCombo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(tr("ocean","Ocean").c_str()));
            SendMessageW(data->hThemeCombo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(tr("forest","Forest").c_str()));
            SendMessageW(data->hThemeCombo, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(tr("custom","Custom").c_str()));
            int tIdx = 0; if (data->cfg.theme=="light") tIdx=1; else if(data->cfg.theme=="midnight") tIdx=2; else if(data->cfg.theme=="ocean") tIdx=3; else if(data->cfg.theme=="forest") tIdx=4; else if(data->cfg.theme=="custom") tIdx=5;
            SendMessageW(data->hThemeCombo, CB_SETCURSEL, tIdx, 0);
            CreateWindowExW(0, L"STATIC", tr("palette","Palette (for Custom)").c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 16, 256, 400, 16, hwnd, nullptr, inst, nullptr);
            data->hBgBtn = CreateWindowExW(0, L"BUTTON", tr("background","Background").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 278, 195, 28, hwnd, reinterpret_cast<HMENU>(IDC_BG_BTN), inst, nullptr);
            data->hPanelBtn = CreateWindowExW(0, L"BUTTON", tr("panel_label","Panel").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 225, 278, 195, 28, hwnd, reinterpret_cast<HMENU>(IDC_PANEL_BTN), inst, nullptr);
            data->hTextBtn = CreateWindowExW(0, L"BUTTON", tr("text_label","Text").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 314, 195, 28, hwnd, reinterpret_cast<HMENU>(IDC_TEXT_BTN), inst, nullptr);
            data->hAccentBtn = CreateWindowExW(0, L"BUTTON", tr("accent_label","Accent").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 225, 314, 195, 28, hwnd, reinterpret_cast<HMENU>(IDC_ACCENT_BTN), inst, nullptr);
            data->hButtonBtn = CreateWindowExW(0, L"BUTTON", tr("button_label","Button").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 350, 195, 28, hwnd, reinterpret_cast<HMENU>(IDC_BUTTON_BTN), inst, nullptr);
            data->hGlowChk = CreateWindowExW(0, L"BUTTON", tr("glow_enable","Enable hover glow").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 16, 386, 400, 22, hwnd, reinterpret_cast<HMENU>(IDC_CHK_GLOW), inst, nullptr);
            CreateWindowExW(0, L"STATIC", tr("glow_color","Glow color").c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 16, 414, 90, 20, hwnd, nullptr, inst, nullptr);
            data->hGlowBtn = CreateWindowExW(0, L"BUTTON", tr("glow_color","Glow color").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 110, 412, 195, 28, hwnd, reinterpret_cast<HMENU>(IDC_GLOW_BTN), inst, nullptr);
            data->hPreview = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_OWNERDRAW | WS_BORDER, 16, 448, 404, 90, hwnd, reinterpret_cast<HMENU>(IDC_PREVIEW), inst, nullptr);
            data->hSysHeader = CreateWindowExW(0, L"STATIC", fromUtf8(lang("system_info")).c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 16, 550, 400, 16, hwnd, nullptr, inst, nullptr);
            data->hSysInfo = CreateWindowExW(0, L"STATIC", getSystemInfoText().c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 16, 570, 404, 100, hwnd, reinterpret_cast<HMENU>(IDC_SYS_INFO), inst, nullptr);
            std::wstring buildText = fromUtf8(lang("build")) + L": " + fromUtf8(RAVEX_BUILD);
            data->hBuild = CreateWindowExW(0, L"STATIC", buildText.c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 16, 680, 200, 16, hwnd, nullptr, inst, nullptr);
            data->hSave = CreateWindowExW(0, L"BUTTON", tr("save","Save").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 260, 700, 68, 28, hwnd, reinterpret_cast<HMENU>(IDOK), inst, nullptr);
            data->hCancel = CreateWindowExW(0, L"BUTTON", tr("cancel","Cancel").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW, 340, 700, 68, 28, hwnd, reinterpret_cast<HMENU>(IDCANCEL), inst, nullptr);
            for (HWND c : {data->hUpdate, data->hSnap, data->hBeta, data->hAlpha, data->hLogs, data->hGlowChk, data->hLangCombo, data->hThemeCombo, data->hSave, data->hCancel, data->hSysHeader, data->hSysInfo, data->hBuild}) SendMessageW(c, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            for (HWND b : {data->hSave, data->hCancel, data->hBgBtn, data->hPanelBtn, data->hTextBtn, data->hAccentBtn, data->hButtonBtn, data->hGlowBtn}) SetWindowTheme(b, L"", nullptr);
            SetWindowTheme(data->hThemeCombo, L"Explorer", nullptr);
            glowCreate(hwnd, &data->glow);
            glowSetButtons(&data->glow, {data->hSave, data->hCancel, data->hBgBtn, data->hPanelBtn, data->hTextBtn, data->hAccentBtn});
            SetTimer(hwnd, 99, 16, nullptr);
            updateThemeState(data);
            return 0;
        }
        case WM_MOUSEMOVE:
            glowUpdate(&data->glow);
            return 0;
        case WM_TIMER:
            if (wParam == 99) glowUpdate(&data->glow);
            return 0;
        case WM_MEASUREITEM: {
            MEASUREITEMSTRUCT* m = reinterpret_cast<MEASUREITEMSTRUCT*>(lParam);
            if (m->CtlID == IDC_LANG_COMBO) { m->itemHeight = 20; return TRUE; }
            return FALSE;
        }
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds = reinterpret_cast<DRAWITEMSTRUCT*>(lParam);
            if (!data) return FALSE;
            if (ds->CtlID == IDOK || ds->CtlID == IDCANCEL) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool dis = (ds->itemState & ODS_DISABLED) != 0;
                bool sel = (ds->itemState & ODS_SELECTED) != 0;
                bool hot = (ds->itemState & ODS_HOTLIGHT) != 0;
                COLORREF base = data->cur.buttonBg;
                if (sel) base = RGB(60,60,62); else if (hot) base = RGB(55,55,57);
                if (dis) base = RGB(54,54,58);
                Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                HBRUSH bgBr = CreateSolidBrush(base); FillRect(dc, &rc, bgBr); DeleteObject(bgBr);
                Gdiplus::SolidBrush br(Gdiplus::Color(GetRValue(base),GetGValue(base),GetBValue(base)));
                Gdiplus::GraphicsPath path; path.AddArc(rc.left,rc.top,8,8,180,90); path.AddArc(rc.right-8,rc.top,8,8,270,90); path.AddArc(rc.right-8,rc.bottom-8,8,8,0,90); path.AddArc(rc.left,rc.bottom-8,8,8,90,90); path.CloseFigure();
                g.FillPath(&br,&path);
                wchar_t txt[64]; GetWindowTextW(ds->hwndItem, txt, 64);
                if(wcslen(txt)>0){
                 Gdiplus::SolidBrush tbr(Gdiplus::Color(255,255,255));
                 Gdiplus::Font f(dc, data->font);
                 Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentCenter); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                 Gdiplus::RectF rf((Gdiplus::REAL)rc.left,(Gdiplus::REAL)rc.top,(Gdiplus::REAL)(rc.right-rc.left),(Gdiplus::REAL)(rc.bottom-rc.top));
                 g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                 g.DrawString(txt,-1,&f,rf,&fmt,&tbr);
                }
                return TRUE;
            }
            if (ds->CtlID == IDC_LANG_COMBO) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool sel = (ds->itemState & ODS_SELECTED) != 0;
                COLORREF bg = sel ? data->cur.accent : data->cur.bg;
                COLORREF fg = sel ? RGB(255,255,255) : data->cur.text;
                HBRUSH b = CreateSolidBrush(bg); FillRect(dc, &rc, b); DeleteObject(b);
                if (ds->itemID == (UINT)-1) return TRUE;
                int langIdx = (int)ds->itemData;
                HBITMAP hb = (langIdx >=0 && langIdx < (int)data->flagBmps.size()) ? data->flagBmps[langIdx] : nullptr;
                int fx = rc.left + 6;
                if (hb) {
                    HDC mdc = CreateCompatibleDC(dc);
                    HBITMAP old = (HBITMAP)SelectObject(mdc, hb);
                    BITMAP bm{}; GetObjectW(hb, sizeof(bm), &bm);
                    BitBlt(dc, fx, rc.top + (rc.bottom-rc.top - 16)/2, 24, 16, mdc, 0, 0, SRCCOPY);
                    SelectObject(mdc, old); DeleteDC(mdc);
                    fx += 30;
                }
                const char* code = (langIdx>=0) ? ravex::langCodeByIndex(langIdx) : "";
                const char* name = ravex::langDisplayName(code);
                SetBkMode(dc, TRANSPARENT); SetTextColor(dc, fg);
                HFONT old = (HFONT)SelectObject(dc, data->font);
                RECT tr = {fx, rc.top, rc.right - 4, rc.bottom};
                DrawTextW(dc, fromUtf8(name).c_str(), -1, &tr, DT_LEFT|DT_VCENTER|DT_SINGLELINE);
                SelectObject(dc, old);
                if (ds->itemState & ODS_FOCUS) DrawFocusRect(dc, &rc);
                return TRUE;
            }
            if (ds->CtlID == IDC_BG_BTN || ds->CtlID == IDC_PANEL_BTN || ds->CtlID == IDC_TEXT_BTN || ds->CtlID == IDC_ACCENT_BTN || ds->CtlID == IDC_BUTTON_BTN || ds->CtlID == IDC_GLOW_BTN) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                COLORREF col = data->cur.bg;
                auto trBtn=[&](const char* k,const char* fb){const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
                std::wstring label;
                if (ds->CtlID == IDC_BG_BTN) { col = data->cur.bg; label = trBtn("background","Background"); }
                else if (ds->CtlID == IDC_PANEL_BTN) { col = data->cur.panel; label = trBtn("panel_label","Panel"); }
                else if (ds->CtlID == IDC_TEXT_BTN) { col = data->cur.text; label = trBtn("text_label","Text"); }
                else if (ds->CtlID == IDC_ACCENT_BTN) { col = data->cur.accent; label = trBtn("accent_label","Accent"); }
                else if (ds->CtlID == IDC_BUTTON_BTN) { col = data->cur.buttonBg; label = trBtn("button_label","Button"); }
                else if (ds->CtlID == IDC_GLOW_BTN) { col = data->cur.glow; label = trBtn("glow_color","Glow color"); }
                bool dis = !IsWindowEnabled(ds->hwndItem);
                HBRUSH bg = CreateSolidBrush(data->cur.bg); FillRect(dc, &rc, bg); DeleteObject(bg);
                RECT sw = {rc.left+8, rc.top+5, rc.left+32, rc.bottom-5};
                HBRUSH swb = CreateSolidBrush(col); HPEN oldP=(HPEN)SelectObject(dc,GetStockObject(NULL_PEN));
                HBRUSH oldB = (HBRUSH)SelectObject(dc, swb);
                Rectangle(dc, sw.left, sw.top, sw.right, sw.bottom);
                SelectObject(dc, oldB); SelectObject(dc, oldP); DeleteObject(swb);
                SetBkMode(dc, TRANSPARENT); SetTextColor(dc, dis?RGB(120,120,120):contrastText(data->cur.bg));
                HFONT old=(HFONT)SelectObject(dc, data->font);
                RECT tr={sw.right+8, rc.top, rc.right-4, rc.bottom};
                DrawTextW(dc, label.c_str(), -1, &tr, DT_LEFT|DT_VCENTER|DT_SINGLELINE);
                SelectObject(dc, old);
                return TRUE;
            }
            if (ds->CtlID == IDC_CHK_UPDATE || ds->CtlID == IDC_CHK_SNAP || ds->CtlID == IDC_CHK_BETA || ds->CtlID == IDC_CHK_ALPHA || ds->CtlID == IDC_CHK_LOGS || ds->CtlID == IDC_CHK_GLOW) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool chk = (ds->CtlID == IDC_CHK_UPDATE) ? data->cUpdate : (ds->CtlID == IDC_CHK_SNAP) ? data->cSnap : (ds->CtlID == IDC_CHK_BETA) ? data->cBeta : (ds->CtlID == IDC_CHK_ALPHA) ? data->cAlpha : (ds->CtlID == IDC_CHK_GLOW) ? data->cGlow : data->cLogs;
                auto tr = [&](const char* key, const char* fallback){ const char* v=lang(key); if(!v||strcmp(v,key)==0) v=fallback; return fromUtf8(v); };
                std::wstring wlabel;
                if (ds->CtlID == IDC_CHK_UPDATE) wlabel = tr("check_update_on_start", "Check for updates on start");
                else if (ds->CtlID == IDC_CHK_SNAP) wlabel = tr("show_snapshots", "Show snapshots");
                else if (ds->CtlID == IDC_CHK_BETA) wlabel = tr("show_beta", "Show beta versions");
                else if (ds->CtlID == IDC_CHK_ALPHA) wlabel = tr("show_alpha", "Show alpha versions");
                else if (ds->CtlID == IDC_CHK_GLOW) wlabel = tr("glow_enable", "Enable hover glow");
                else wlabel = tr("save_logs", "Save game logs");
                const wchar_t* label = wlabel.c_str();
                HBRUSH bg = CreateSolidBrush(data->cur.bg); FillRect(dc, &rc, bg); DeleteObject(bg);
                int box = 16; int bx = rc.left + 2; int by = rc.top + (rc.bottom - rc.top - box)/2;
                Gdiplus::Graphics g(dc);
                g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                Gdiplus::Color fill = chk ? Gdiplus::Color(255, GetRValue(data->cur.accent), GetGValue(data->cur.accent), GetBValue(data->cur.accent)) : Gdiplus::Color(255, GetRValue(data->cur.panel), GetGValue(data->cur.panel), GetBValue(data->cur.panel));
                Gdiplus::Color bord = chk ? Gdiplus::Color(255, GetRValue(data->cur.accent), GetGValue(data->cur.accent), GetBValue(data->cur.accent)) : Gdiplus::Color(255, 225, 225, 225);
                Gdiplus::SolidBrush br(fill);
                Gdiplus::Pen pen(bord, 1.0f);
                Gdiplus::GraphicsPath path; path.AddArc(bx, by, 4,4,180,90); path.AddArc(bx+box-4, by,4,4,270,90); path.AddArc(bx+box-4, by+box-4,4,4,0,90); path.AddArc(bx, by+box-4,4,4,90,90); path.CloseFigure();
                g.FillPath(&br, &path); g.DrawPath(&pen, &path);
                if (chk) {
                    Gdiplus::Pen cpen(Gdiplus::Color(255,255,255,255), 2.0f); cpen.SetLineCap(Gdiplus::LineCapRound,Gdiplus::LineCapRound,Gdiplus::DashCapFlat);
                    g.DrawLine(&cpen, bx+4, by+8, bx+7, by+12); g.DrawLine(&cpen, bx+7, by+12, bx+13, by+4);
                }
                Gdiplus::SolidBrush tbr(Gdiplus::Color(255,255,255,255));
                Gdiplus::Font f(dc, data->font);
                Gdiplus::RectF rf((Gdiplus::REAL)(bx+box+8), (Gdiplus::REAL)rc.top, (Gdiplus::REAL)(rc.right - (bx+box+8)), (Gdiplus::REAL)(rc.bottom - rc.top));
                Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentNear); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter); fmt.SetTrimming(Gdiplus::StringTrimmingEllipsisCharacter); fmt.SetFormatFlags(Gdiplus::StringFormatFlagsNoWrap);
                g.DrawString(label, -1, &f, rf, &fmt, &tbr);
                return TRUE;
            }
            if (ds->CtlID == IDC_PREVIEW) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                HBRUSH bg = CreateSolidBrush(data->cur.bg); FillRect(dc, &rc, bg); DeleteObject(bg);
                RECT panel = {rc.left + 10, rc.top + 10, rc.right - 10, rc.top + 38};
                HBRUSH pb = CreateSolidBrush(data->cur.panel); FillRect(dc, &panel, pb); DeleteObject(pb);
                SetBkMode(dc, TRANSPARENT); SetTextColor(dc, contrastText(data->cur.bg));
                HFONT old = (HFONT)SelectObject(dc, data->font);
                DrawTextW(dc, L"KickX Launcher - Preview", -1, &panel, DT_CENTER|DT_VCENTER|DT_SINGLELINE);
                SelectObject(dc, old);
                RECT acc = {rc.left + 10, rc.bottom - 28, rc.right - 10, rc.bottom - 10};
                HBRUSH ab = CreateSolidBrush(data->cur.accent); FillRect(dc, &acc, ab); DeleteObject(ab);
                HPEN pen = CreatePen(PS_SOLID,1,data->cur.text); HBRUSH oldB=(HBRUSH)SelectObject(dc, GetStockObject(NULL_BRUSH)); HPEN oldP=(HPEN)SelectObject(dc, pen);
                Rectangle(dc, rc.left, rc.top, rc.right, rc.bottom);
                SelectObject(dc, oldB); SelectObject(dc, oldP); DeleteObject(pen);
                return TRUE;
            }
            return FALSE;
        }
        case WM_COMMAND:
            if (HIWORD(wParam) == CBN_SELCHANGE && LOWORD(wParam) == IDC_LANG_COMBO) {
                int idx=(int)SendMessageW(data->hLangCombo, CB_GETCURSEL, 0, 0);
                if(idx>=0){int real=(int)SendMessageW(data->hLangCombo, CB_GETITEMDATA, idx, 0); if(real>=0&&real<ravex::langCount()){const char* code=ravex::langCodeByIndex(real); SetWindowTextW(data->hSysHeader, fromUtf8(langFor("system_info",code)).c_str()); SetWindowTextW(data->hSysInfo, getSystemInfoTextForLang(code).c_str()); SetWindowTextW(data->hBuild, (fromUtf8(langFor("build",code)) + L": " + fromUtf8(RAVEX_BUILD)).c_str());}}
                return 0;
            }
            if (HIWORD(wParam) == CBN_SELCHANGE && LOWORD(wParam) == IDC_THEME_COMBO) { applyThemeChoice(data); return 0; }
            if (LOWORD(wParam) == IDC_CHK_UPDATE) { data->cUpdate = !data->cUpdate; InvalidateRect(data->hUpdate, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_CHK_SNAP) { data->cSnap = !data->cSnap; InvalidateRect(data->hSnap, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_CHK_BETA) { data->cBeta = !data->cBeta; InvalidateRect(data->hBeta, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_CHK_ALPHA) { data->cAlpha = !data->cAlpha; InvalidateRect(data->hAlpha, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_CHK_LOGS) { data->cLogs = !data->cLogs; InvalidateRect(data->hLogs, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_CHK_GLOW) { data->cGlow = !data->cGlow; InvalidateRect(data->hGlowChk, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_BG_BTN) { COLORREF c = pickColor(hwnd, data->cfg.customBg, &data->cfg.customAlpha); data->cfg.customBg = c; data->cur.bg = c; if(data->bgBrush) DeleteObject(data->bgBrush); data->bgBrush = CreateSolidBrush(c); InvalidateRect(hwnd, nullptr, TRUE); for(HWND b : {data->hBgBtn, data->hPanelBtn, data->hTextBtn, data->hAccentBtn, data->hButtonBtn, data->hPreview}) if(b) InvalidateRect(b, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_PANEL_BTN) { COLORREF c = pickColor(hwnd, data->cfg.customPanel, &data->cfg.customAlpha); data->cfg.customPanel = c; data->cur.panel = c; InvalidateRect(data->hPreview, nullptr, TRUE); InvalidateRect(data->hPanelBtn, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_TEXT_BTN) { COLORREF c = pickColor(hwnd, data->cfg.customText, &data->cfg.customAlpha); data->cfg.customText = c; data->cur.text = c; InvalidateRect(data->hPreview, nullptr, TRUE); InvalidateRect(data->hTextBtn, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_ACCENT_BTN) { COLORREF c = pickColor(hwnd, data->cfg.customAccent, &data->cfg.customAlpha); data->cfg.customAccent = c; data->cur.accent = c; InvalidateRect(data->hPreview, nullptr, TRUE); InvalidateRect(data->hAccentBtn, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_BUTTON_BTN) { COLORREF c = pickColor(hwnd, data->cfg.customButton ? data->cfg.customButton : data->cfg.customAccent, &data->cfg.customAlpha); data->cfg.customButton = c; data->cur.buttonBg = c; InvalidateRect(data->hButtonBtn, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDC_GLOW_BTN) { COLORREF c = pickColor(hwnd, data->cfg.customGlow, &data->cfg.customAlpha); data->cfg.customGlow = c; data->cur.glow = c; InvalidateRect(data->hGlowBtn, nullptr, TRUE); return 0; }
            if (LOWORD(wParam) == IDOK) {
                data->cfg.checkUpdatesOnStart = data->cUpdate;
                data->cfg.showSnapshots = data->cSnap;
                data->cfg.showBeta = data->cBeta;
                data->cfg.showAlpha = data->cAlpha;
                data->cfg.saveLogs = data->cLogs;
                int langIdx = (int)SendMessageW(data->hLangCombo, CB_GETCURSEL, 0, 0);
                if (langIdx >=0) { int real = (int)SendMessageW(data->hLangCombo, CB_GETITEMDATA, langIdx, 0); if (real>=0 && real < ravex::langCount()) data->cfg.language = ravex::langCodeByIndex(real); }
                int tSel = (int)SendMessageW(data->hThemeCombo, CB_GETCURSEL, 0, 0);
                if (tSel==0) data->cfg.theme="dark"; else if(tSel==1) data->cfg.theme="light"; else if(tSel==2) data->cfg.theme="midnight"; else if(tSel==3) data->cfg.theme="ocean"; else if(tSel==4) data->cfg.theme="forest"; else data->cfg.theme="custom";
                data->cfg.customBg = data->cur.bg; data->cfg.customPanel = data->cur.panel; data->cfg.customText = data->cur.text; data->cfg.customAccent = data->cur.accent; data->cfg.customButton = data->cur.buttonBg; data->cfg.customGlow = data->cur.glow; data->cfg.glowEnabled = data->cGlow; data->cfg.customAlpha = data->cur.colorAlpha;
                data->ok = true; data->closed = true; DestroyWindow(hwnd); return 0;
            }
            if (LOWORD(wParam) == IDCANCEL) { data->closed = true; DestroyWindow(hwnd); return 0; }
            return 0;
        case WM_CTLCOLORSTATIC: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            HWND ctrl = reinterpret_cast<HWND>(lParam);
            if (ctrl == data->hPreview) return reinterpret_cast<LRESULT>(GetStockObject(NULL_BRUSH));
            SetBkColor(dc, data->cur.bg); SetTextColor(dc, contrastText(data->cur.bg));
            return reinterpret_cast<LRESULT>(data->bgBrush);
        }
        case WM_CTLCOLORBTN: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, data->cur.bg); SetTextColor(dc, RGB(238, 238, 238));
            return reinterpret_cast<LRESULT>(data->bgBrush);
        }
        case WM_ERASEBKGND: {
            HDC dc = reinterpret_cast<HDC>(wParam); RECT rc; GetClientRect(hwnd, &rc);
            FillRect(dc, &rc, data->bgBrush); return 1;
        }
        case WM_CLOSE: data->closed = true; DestroyWindow(hwnd); return 0;
        case WM_DESTROY:
            KillTimer(hwnd, 99);
            glowDestroy(&data->glow);
            for (HBITMAP hb : data->flagBmps) if (hb) DeleteObject(hb);
            if (data->font) DeleteObject(data->font);
            if (data->smallFont) DeleteObject(data->smallFont);
            if (data->bgBrush) DeleteObject(data->bgBrush);
            if (data->gdiToken) Gdiplus::GdiplusShutdown(data->gdiToken);
            data->closed = true; return 0;
        default: return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}
}
bool showSettingsDialog(HWND parent) {
    LauncherConfig cfg = loadLauncherConfig();
    setCurrentLanguage(cfg.language.c_str());
    SettingsData data; data.cfg = cfg;
    data.cur = getThemeForConfig(cfg.theme, cfg.customBg, cfg.customPanel, cfg.customText, cfg.customAccent, 0, 255, cfg.customGlow, cfg.glowEnabled);
    data.selected = cfg.theme;
    HINSTANCE inst = reinterpret_cast<HINSTANCE>(GetWindowLongPtrW(parent, GWLP_HINSTANCE)); if (!inst) inst = GetModuleHandleW(nullptr);
    static bool reg = false;
    if (!reg) { WNDCLASSEXW wc{}; wc.cbSize=sizeof(wc); wc.lpfnWndProc=SettingsProc; wc.hInstance=inst; wc.hIcon=LoadIconW(inst, MAKEINTRESOURCEW(101)); if(!wc.hIcon) wc.hIcon=LoadIconW(nullptr, MAKEINTRESOURCEW(32512)); wc.hIconSm=wc.hIcon; wc.hCursor=LoadCursorW(nullptr, MAKEINTRESOURCEW(32512)); wc.lpszClassName=L"KickXSettings"; RegisterClassExW(&wc); reg=true; }
    RECT pr{}; GetWindowRect(parent, &pr); int pw=pr.right-pr.left; int ph=pr.bottom-pr.top;
    RECT cr{0,0,440,740}; AdjustWindowRectEx(&cr, WS_POPUP|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MAXIMIZEBOX, FALSE, 0);
    int dw=cr.right-cr.left; int dh=cr.bottom-cr.top;
    int x=pr.left+(pw-dw)/2; int y=pr.top+(ph-dh)/2;
    HMONITOR mon=MonitorFromWindow(parent, MONITOR_DEFAULTTONEAREST); MONITORINFO mi{}; mi.cbSize=sizeof(mi); GetMonitorInfoW(mon,&mi); RECT wr=mi.rcWork;
    if(x<wr.left) x=wr.left+16; if(y<wr.top) y=wr.top+16;
    if(x+dw>wr.right) x=wr.right-dw-16; if(y+dh>wr.bottom) y=wr.bottom-dh-16;
    HWND hwnd=CreateWindowExW(0, L"KickXSettings", L"Settings", WS_POPUP|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MAXIMIZEBOX, x, y, dw, dh, parent, nullptr, inst, &data);
    if(!hwnd) return false;
    DwmSetWindowAttribute(hwnd, 20, &data.cur.isDark, sizeof(data.cur.isDark));
    ShowWindow(hwnd, SW_SHOW); UpdateWindow(hwnd);
    EnableWindow(parent, FALSE);
    MSG msg; while(!data.closed){ while(PeekMessageW(&msg,nullptr,0,0,PM_REMOVE)){ if(IsDialogMessageW(hwnd,&msg)) continue; TranslateMessage(&msg); DispatchMessageW(&msg);} Sleep(10); }
    EnableWindow(parent, TRUE); SetForegroundWindow(parent);
    if(data.ok){ saveLauncherConfig(data.cfg); return true; }
    return false;
}
}


