#include "ui/include/main_window.hpp"
#include <windows.h>
#include <windowsx.h>
#include <dwmapi.h>
#include <commctrl.h>
#include <uxtheme.h>
#include <commdlg.h>
#include <shellapi.h>
#include <gdiplus.h>
#include <cwchar>
#include <cstdlib>
#include <cmath>
#include <cctype>
#include <algorithm>
#include <initializer_list>
#include <unordered_map>
#include <string>
#include <thread>
#include <utility>
#include <vector>
#include <fstream>
#include "core/include/config.hpp"
#include "core/include/lang.hpp"
#include "core/include/paths.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "net/include/http.hpp"
#include "game/include/accounts.hpp"
#include "game/include/fabric.hpp"
#include "game/include/java.hpp"
#include "game/include/launch.hpp"
#include "game/include/mojang.hpp"
#include "game/include/ravex.hpp"
#include "ui/include/console_window.hpp"
#include "ui/include/instance_editor.hpp"
#include "ui/include/account_dialog.hpp"
#include "ui/include/settings_dialog.hpp"
#include "ui/include/glow.hpp"

namespace ravex::ui {
constexpr int IDC_INSTANCES = 1001;
constexpr int IDC_ACCOUNTS = 1002;
constexpr int IDC_STATUS = 1003;
constexpr int IDC_PROGRESS = 1004;
constexpr int IDC_LAUNCH = 1005;
constexpr int IDC_UPDATE = 1006;
constexpr int IDC_CONSOLE = 1007;
constexpr int IDC_ADD_INST = 1008;
constexpr int IDC_EDIT_INST = 1009;
constexpr int IDC_DEL_INST = 1010;
constexpr int IDC_ADD_OFFLINE = 1012;
constexpr int IDC_ADD_MS = 1013;
constexpr int IDC_SETTINGS = 1014;
constexpr int IDC_WINLABEL = 1015;
constexpr int IDC_ACCOUNTS_ICON = 1016;
constexpr int IDC_SEARCH_INST = 1017;
constexpr int IDC_SORT_INST = 1018;
constexpr int IDC_SKIN_PREVIEW = 1019;

constexpr UINT WM_APP_STATUS = WM_APP + 1;
constexpr UINT WM_APP_PROGRESS = WM_APP + 2;
constexpr UINT WM_APP_JOB_DONE = WM_APP + 3;
constexpr UINT WM_APP_CONSOLE_LINE = WM_APP + 4;
constexpr UINT WM_APP_ACCOUNT_DONE = WM_APP + 5;
constexpr UINT WM_APP_GAME_RUNNING = WM_APP + 6;
constexpr UINT WM_APP_GAME_EXITED = WM_APP + 7;
constexpr UINT WM_APP_SKIN_DONE = WM_APP + 20;

struct JobDone {
    enum class Kind { Update, Launch };
    Kind kind;
    bool ok;
    std::string error;
    std::string extra;
};

struct AccountDone {
    bool ok;
    std::string error;
    Account account;
};

struct MainData {
    HWND hwnd = nullptr;
    HFONT font = nullptr;
    HFONT iconFont = nullptr;
    HBRUSH bgBrush = nullptr;
    HBRUSH panelBrush = nullptr;
    ThemeColors theme;
    HWND hList = nullptr;
    HWND hAccounts = nullptr;
    HWND hStatus = nullptr;
    HWND hProgress = nullptr;
    HWND hLaunch = nullptr;
    HWND hUpdate = nullptr;
    HWND hConsoleBtn = nullptr;
    HWND hAdd = nullptr;
    HWND hEdit = nullptr;
    HWND hDel = nullptr;
    HWND hOffline = nullptr;
    HWND hMs = nullptr;
    HWND hSettings = nullptr;
    HWND hAccountsIcon = nullptr;
    HWND hWinLabel = nullptr;
    HWND hSearch = nullptr;
    HWND hSort = nullptr;
    HWND hSkin = nullptr;
    Gdiplus::Bitmap* skinBmp = nullptr;
    HICON skinIcon = nullptr;
    std::wstring searchFilter;
    int sortMode = 0;
    std::unordered_map<HWND, Gdiplus::Bitmap*> btnBmp;
    std::vector<HWND> ownerBtns;
    std::vector<Gdiplus::Bitmap*> iconBmps;
    Gdiplus::Bitmap* acctIconBmp = nullptr;
    HICON acctIcon = nullptr;
    Gdiplus::Bitmap* winLabelBmp = nullptr;
    GlowData glow;
    LauncherConfig cfg;
    std::vector<InstanceCfg> instances;
    std::vector<InstanceCfg> filteredInstances;
    bool busy = false;
    bool cancelled = false;
    bool gameRunning = false;
    HWND hoveredBtn = nullptr;
};

MainData g_main;
game::GameProcess g_gameProc;
bool g_killRequested = false;

void updateSkinPreview();

bool promptForText(HWND parent, const std::wstring& title, std::wstring& out);

HFONT makeFont() {
    HFONT f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Manrope");
    if (!f) f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    if (!f) f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    return f;
}
HFONT makeIconFont() {
    return CreateFontW(-15, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe MDL2 Assets");
}

void setStatus(const std::string& text) {
    SetWindowTextW(g_main.hStatus, fromUtf8(text).c_str());
}

void setProgress(int percent) {
    SendMessageW(g_main.hProgress, PBM_SETMARQUEE, FALSE, 0);
    SendMessageW(g_main.hProgress, PBM_SETPOS, percent, 0);
}
void showProgress(bool show) {
    ShowWindow(g_main.hProgress, show ? SW_SHOW : SW_HIDE);
}

COLORREF brighten(COLORREF c, int amount) {
    int r = GetRValue(c) + amount;
    int g = GetGValue(c) + amount;
    int b = GetBValue(c) + amount;
    if (r > 255) r = 255;
    if (g > 255) g = 255;
    if (b > 255) b = 255;
    return RGB(r, g, b);
}

std::wstring iconPath(const std::wstring& name) {
    wchar_t buf[MAX_PATH];
    GetModuleFileNameW(nullptr, buf, MAX_PATH);
    std::wstring s = buf;
    size_t p = s.find_last_of(L"\\/");
    if (p != std::wstring::npos) s = s.substr(0, p + 1);
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

Gdiplus::Bitmap* loadIconBmp(const std::wstring& path, int target) {
    Gdiplus::Bitmap* src = Gdiplus::Bitmap::FromFile(path.c_str());
    if (!src || src->GetLastStatus() != Gdiplus::Ok) { delete src; return nullptr; }
    Gdiplus::Bitmap* dst = new Gdiplus::Bitmap(target, target, PixelFormat32bppARGB);
    Gdiplus::Graphics g(dst);
    g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
    g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
    g.SetPixelOffsetMode(Gdiplus::PixelOffsetModeHighQuality);
    g.SetCompositingQuality(Gdiplus::CompositingQualityHighQuality);
    g.Clear(Gdiplus::Color(0,0,0,0));
    g.DrawImage(src, Gdiplus::Rect(0,0,target,target), 0,0, src->GetWidth(), src->GetHeight(), Gdiplus::UnitPixel);
    delete src;
    return dst;
}

void refreshAccounts() {
    if (!g_main.hAccounts) return;
    SendMessageW(g_main.hAccounts, CB_RESETCONTENT, 0, 0);
    if (g_main.cfg.accounts.empty()) {
        std::wstring placeholder = fromUtf8(lang("add_account_hint"));
        SendMessageW(g_main.hAccounts, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(placeholder.c_str()));
        SendMessageW(g_main.hAccounts, CB_SETCURSEL, 0, 0);
        SendMessageW(g_main.hAccounts, CB_SETITEMHEIGHT, -1, 24);
        EnableWindow(g_main.hAccounts, FALSE);
        EnableWindow(g_main.hLaunch, FALSE);
        InvalidateRect(g_main.hAccounts, nullptr, TRUE);
        UpdateWindow(g_main.hAccounts);
        updateSkinPreview();
        return;
    }
    EnableWindow(g_main.hAccounts, TRUE);
    if (!g_main.busy && !g_main.gameRunning) EnableWindow(g_main.hLaunch, TRUE);
    for (const Account& account : g_main.cfg.accounts) {
        std::string label = account.name;
        if (!account.type.empty()) label += " (" + account.type + ")";
        std::wstring wlabel = fromUtf8(label);
        SendMessageW(g_main.hAccounts, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(wlabel.c_str()));
    }
    if (g_main.cfg.activeAccount >= 0 &&
        g_main.cfg.activeAccount < static_cast<int>(g_main.cfg.accounts.size())) {
        SendMessageW(g_main.hAccounts, CB_SETCURSEL, g_main.cfg.activeAccount, 0);
    } else {
        SendMessageW(g_main.hAccounts, CB_SETCURSEL, 0, 0);
        g_main.cfg.activeAccount = 0;
    }
    SendMessageW(g_main.hAccounts, CB_SETITEMHEIGHT, -1, 24);
    InvalidateRect(g_main.hAccounts, nullptr, TRUE);
    UpdateWindow(g_main.hAccounts);
    updateSkinPreview();
}

void refreshInstances() {
    g_main.instances = listInstances();
    std::wstring filter = g_main.searchFilter;
    std::string filter8 = toUtf8(filter);
    std::transform(filter8.begin(), filter8.end(), filter8.begin(), ::tolower);
    g_main.filteredInstances.clear();
    for (auto &inst : g_main.instances) {
        if (!filter8.empty()) {
            std::string hay = inst.name + " " + inst.mcVersion + " " + inst.loader;
            std::string low = hay;
            std::transform(low.begin(), low.end(), low.begin(), ::tolower);
            if (low.find(filter8) == std::string::npos) continue;
        }
        g_main.filteredInstances.push_back(inst);
    }
    auto verCompare = [](const std::string& a, const std::string& b){
        std::vector<int> pa, pb;
        std::string cur;
        for(char c: a){ if(c=='.'){ if(!cur.empty()){pa.push_back(std::stoi(cur)); cur.clear();}} else if(isdigit((unsigned char)c)) cur+=c; else {if(!cur.empty()){pa.push_back(std::stoi(cur)); cur.clear();} break;}} if(!cur.empty()) pa.push_back(std::stoi(cur));
        cur.clear();
        for(char c: b){ if(c=='.'){ if(!cur.empty()){pb.push_back(std::stoi(cur)); cur.clear();}} else if(isdigit((unsigned char)c)) cur+=c; else {if(!cur.empty()){pb.push_back(std::stoi(cur)); cur.clear();} break;}} if(!cur.empty()) pb.push_back(std::stoi(cur));
        size_t n=std::max(pa.size(), pb.size());
        for(size_t i=0;i<n;++i){ int av=i<pa.size()?pa[i]:0; int bv=i<pb.size()?pb[i]:0; if(av!=bv) return av<bv; } return false;
    };
    if (g_main.sortMode == 0) {
        std::sort(g_main.filteredInstances.begin(), g_main.filteredInstances.end(), [](auto &a, auto &b){ std::string la=a.name, lb=b.name; std::transform(la.begin(), la.end(), la.begin(), ::tolower); std::transform(lb.begin(), lb.end(), lb.begin(), ::tolower); return la<lb; });
    } else if (g_main.sortMode == 1) {
        std::sort(g_main.filteredInstances.begin(), g_main.filteredInstances.end(), [](auto &a, auto &b){ std::string la=a.name, lb=b.name; std::transform(la.begin(), la.end(), la.begin(), ::tolower); std::transform(lb.begin(), lb.end(), lb.begin(), ::tolower); return la>lb; });
    } else if (g_main.sortMode == 2) {
        std::sort(g_main.filteredInstances.begin(), g_main.filteredInstances.end(), [&](auto &a, auto &b){ return verCompare(b.mcVersion, a.mcVersion); });
    } else if (g_main.sortMode == 3) {
        auto getTime = [](const InstanceCfg& c){ std::wstring dir = instanceDir(fromUtf8(c.name)); WIN32_FILE_ATTRIBUTE_DATA fad{}; if(GetFileAttributesExW(dir.c_str(), GetFileExInfoStandard, &fad)){ ULARGE_INTEGER t; t.LowPart=fad.ftLastWriteTime.dwLowDateTime; t.HighPart=fad.ftLastWriteTime.dwHighDateTime; return t.QuadPart; } return (ULONGLONG)0; };
        std::sort(g_main.filteredInstances.begin(), g_main.filteredInstances.end(), [&](auto &a, auto &b){ return getTime(a) > getTime(b); });
    }
    SendMessageW(g_main.hList, LB_RESETCONTENT, 0, 0);
    for (const InstanceCfg& inst : g_main.filteredInstances) {
        std::string label = inst.name;
        if (!inst.mcVersion.empty()) label += " [" + inst.mcVersion + "]";
        if (!inst.loader.empty() && inst.loader != "vanilla") label += " (" + inst.loader + ")";
        std::wstring wlabel = fromUtf8(label);
        SendMessageW(g_main.hList, LB_ADDSTRING, 0, reinterpret_cast<LPARAM>(wlabel.c_str()));
    }
    if (!g_main.filteredInstances.empty()) SendMessageW(g_main.hList, LB_SETCURSEL, 0, 0);
}

int selectedInstanceIndex() {
    int idx = static_cast<int>(SendMessageW(g_main.hList, LB_GETCURSEL, 0, 0));
    if (idx < 0 || idx >= static_cast<int>(g_main.filteredInstances.size())) return -1;
    return idx;
}
InstanceCfg* selectedInstance() {
    int idx = selectedInstanceIndex();
    if (idx < 0) return nullptr;
    return &g_main.filteredInstances[idx];
}

void updateSkinPreview() {
    if (!g_main.hSkin) return;
    if (g_main.skinBmp) { delete g_main.skinBmp; g_main.skinBmp = nullptr; }
    if (g_main.skinIcon) { DestroyIcon(g_main.skinIcon); g_main.skinIcon = nullptr; }
    if (g_main.cfg.accounts.empty() || g_main.cfg.activeAccount < 0 || g_main.cfg.activeAccount >= (int)g_main.cfg.accounts.size()) {
        InvalidateRect(g_main.hSkin, nullptr, TRUE);
        return;
    }
    Account acc = g_main.cfg.accounts[g_main.cfg.activeAccount];
    if (acc.type != "microsoft" || acc.uuid.empty()) {
        InvalidateRect(g_main.hSkin, nullptr, TRUE);
        return;
    }
    std::string uuid = acc.uuid;
    uuid.erase(std::remove(uuid.begin(), uuid.end(), '-'), uuid.end());
    std::string url = "https://crafatar.com/avatars/" + uuid + "?size=64&overlay";
    std::thread([url](){
        std::wstring tmp = joinPath(joinPath(ravexDir(), L"cache"), L"skin_tmp.png");
        createDirs(joinPath(ravexDir(), L"cache"));
        bool ok = net::downloadFile(url, tmp, {}, nullptr, nullptr);
        if (!ok) {
            PostMessageW(g_main.hwnd, WM_APP_SKIN_DONE, 0, 0);
            return;
        }
        Gdiplus::Bitmap* src = Gdiplus::Bitmap::FromFile(tmp.c_str());
        if (!src || src->GetLastStatus() != Gdiplus::Ok) { delete src; PostMessageW(g_main.hwnd, WM_APP_SKIN_DONE, 0, 0); return; }
        Gdiplus::Bitmap* dst = new Gdiplus::Bitmap(64, 64, PixelFormat32bppARGB);
        Gdiplus::Graphics g(dst);
        g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
        g.Clear(Gdiplus::Color(0,0,0,0));
        g.DrawImage(src, Gdiplus::Rect(0,0,64,64), 0,0, src->GetWidth(), src->GetHeight(), Gdiplus::UnitPixel);
        delete src;
        PostMessageW(g_main.hwnd, WM_APP_SKIN_DONE, 0, reinterpret_cast<LPARAM>(dst));
    }).detach();
}

void setBusy(bool busy) {
    g_main.busy = busy;
    bool hasAccount = !g_main.cfg.accounts.empty();
    EnableWindow(g_main.hLaunch, !busy && hasAccount && !g_main.gameRunning);
    EnableWindow(g_main.hUpdate, !busy);
    EnableWindow(g_main.hConsoleBtn, !busy);
    EnableWindow(g_main.hAdd, !busy);
    EnableWindow(g_main.hEdit, !busy);
    EnableWindow(g_main.hDel, !busy);
    EnableWindow(g_main.hOffline, !busy);
    EnableWindow(g_main.hMs, !busy);
    EnableWindow(g_main.hSettings, !busy);
    (void)busy;
    if (busy) {
        showProgress(true);
        SendMessageW(g_main.hProgress, PBM_SETMARQUEE, TRUE, 0);
        SetTimer(g_main.hwnd, 1, 340, nullptr);
    } else {
        SendMessageW(g_main.hProgress, PBM_SETMARQUEE, FALSE, 0);
        KillTimer(g_main.hwnd, 1);
        g_main.cancelled = false;
        setProgress(0);
        showProgress(false);
    }
}
void createGlowBitmap() {
    glowCreate(g_main.hwnd, &g_main.glow);
    g_main.glow.anywhere = true;
    g_main.glow.parent = g_main.hwnd;
    if (g_main.glow.bmp) {
        HDC hdcScreen = GetDC(nullptr);
        BITMAPINFO bi{}; bi.bmiHeader.biSize=sizeof(BITMAPINFOHEADER); bi.bmiHeader.biWidth=60; bi.bmiHeader.biHeight=-60; bi.bmiHeader.biPlanes=1; bi.bmiHeader.biBitCount=32; bi.bmiHeader.biCompression=BI_RGB;
        void* pv=nullptr;
        HBITMAP wb = CreateDIBSection(hdcScreen,&bi,DIB_RGB_COLORS,&pv,nullptr,0);
        ReleaseDC(nullptr,hdcScreen);
        if (pv) {
            BYTE* px=(BYTE*)pv;
            for(int y=0;y<60;++y) for(int x=0;x<60;++x){ float dx=x-30,dy=y-30; float d=sqrtf(dx*dx+dy*dy); float t=d/26.0f; if(t>1) t=1; float s=1-t; float a=s*s*s*90; BYTE al=(BYTE)(a>255?255:a); float f=al/255.0f; int o=(y*60+x)*4; px[o+0]=(BYTE)(255*f); px[o+1]=(BYTE)(255*f); px[o+2]=(BYTE)(255*f); px[o+3]=al; }
            DeleteObject(g_main.glow.bmp);
            g_main.glow.bmp=wb;
        }
    }
}
bool isGlowButton(HWND hwnd) {
    for (HWND b : g_main.glow.buttons) if (b == hwnd) return true;
    return false;
}
void moveGlowToCursor() {
    glowUpdateAnywhere(g_main.hwnd, &g_main.glow);
}

void postStatus(const std::string& text) {
    std::wstring* payload = new std::wstring(fromUtf8(text));
    if (!PostMessageW(g_main.hwnd, WM_APP_STATUS, 0, reinterpret_cast<LPARAM>(payload))) delete payload;
}

void postProgress(const net::Progress& p) {
    if (p.total == 0) {
        PostMessageW(g_main.hwnd, WM_APP_PROGRESS, 0, -1);
        return;
    }
    int percent = static_cast<int>((p.downloaded * 100) / p.total);
    if (percent > 100) percent = 100;
    if (!p.url.empty()) {
        static thread_local std::string lastUrl;
        static thread_local int lastBucket = -1;
        int bucket = percent / 5;
        bool fileChanged = (p.url != lastUrl);
        if (fileChanged || bucket != lastBucket) {
            lastUrl = p.url;
            lastBucket = bucket;
            size_t pos = p.url.find_last_of("/\\");
            std::string name = (pos == std::string::npos) ? p.url : p.url.substr(pos + 1);
            size_t q = name.find('?');
            if (q != std::string::npos) name = name.substr(0, q);
            if (name.size() > 48) name = "..." + name.substr(name.size() - 45);
            if (name.empty()) name = "file";
            postStatus(std::string(lang("downloading")) + name + " (" + std::to_string(percent) + "%)");
        }
    }
    PostMessageW(g_main.hwnd, WM_APP_PROGRESS, 0, percent);
}

void postConsoleLine(const std::string& line) {
    std::wstring* payload = new std::wstring(fromUtf8(line));
    if (!PostMessageW(g_main.hwnd, WM_APP_CONSOLE_LINE, 0, reinterpret_cast<LPARAM>(payload))) delete payload;
}

void postJobDone(JobDone* done) {
    if (!PostMessageW(g_main.hwnd, WM_APP_JOB_DONE, 0, reinterpret_cast<LPARAM>(done))) delete done;
}

void postAccountDone(AccountDone* done) {
    if (!PostMessageW(g_main.hwnd, WM_APP_ACCOUNT_DONE, 0, reinterpret_cast<LPARAM>(done))) delete done;
}



void workerUpdate() {
    g_main.cancelled = false;
    postStatus(std::string(lang("fetching_release")));
    game::ReleaseInfo release;
    std::string error;
    if (!game::fetchLatestRelease(&release, &error)) {
        postJobDone(new JobDone{JobDone::Kind::Update, false, error, ""});
        return;
    }
    postStatus(std::string(lang("installing_ravex")) + release.tag + "...");
    if (!game::installRavex(release, &error, postProgress, &g_main.cancelled)) {
        postJobDone(new JobDone{JobDone::Kind::Update, false, error, ""});
        return;
    }
    postJobDone(new JobDone{JobDone::Kind::Update, true, "", release.tag});
}

static void logTelemetry(const std::string& msg) {
    try {
        std::wstring dir = joinPath(kickxDir(), L"logs");
        createDirs(dir);
        std::wstring file = joinPath(dir, L"launcher.log");
        std::string narrow = toUtf8(file);
        std::ofstream ofs(narrow, std::ios::app);
        if (ofs) {
            SYSTEMTIME st; GetLocalTime(&st);
            char buf[64];
            std::snprintf(buf, sizeof(buf), "%04d-%02d-%02d %02d:%02d:%02d", st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond);
            ofs << "[" << buf << "] " << msg << "\n";
        }
    } catch (...) {}
}

static bool isOnline() {
    std::string err;
    std::string r = ravex::net::httpGet("https://launchermeta.mojang.com/", &err);
    return !r.empty();
}

void workerLaunch(const InstanceCfg& inst, const Account& account) {
    logTelemetry("launch start name=" + inst.name + " mc=" + inst.mcVersion + " loader=" + inst.loader);
    g_main.cancelled = false;
    g_killRequested = false;
    std::string error;
    bool fabric = (inst.loader == "fabric");
    int javaVersion = game::requiredJavaVersion(inst.mcVersion);
    std::wstring javaExe;
    bool installed = game::isMinecraftInstalled(inst.mcVersion);
    if (!inst.useBundledJava && !inst.javaPath.empty()) {
        javaExe = fromUtf8(inst.javaPath);
        if (!fileExists(javaExe)) {
            postJobDone(new JobDone{JobDone::Kind::Launch, false, std::string(lang("custom_java_not_found")) + inst.javaPath, ""});
            return;
        }
    } else if (installed) {
        if (!game::ensureJava(javaVersion, javaExe, &error, postStatus, &g_main.cancelled)) {
            postJobDone(new JobDone{JobDone::Kind::Launch, false, error, ""});
            return;
        }
    } else {
        std::wstring javaPathOut;
        postStatus(std::string(lang("checking_java")) + std::to_string(javaVersion) + "...");
        PostMessageW(g_main.hwnd, WM_APP_PROGRESS, 0, -1);
        if (!game::ensureJava(javaVersion, javaPathOut, &error, postStatus, &g_main.cancelled)) {
            postJobDone(new JobDone{JobDone::Kind::Launch, false, error, ""});
            return;
        }
        javaExe = javaPathOut;
    }
    std::string assetIndexId;
    if (!installed) {
        postStatus(std::string(lang("checking_mc")) + inst.mcVersion + "...");
        PostMessageW(g_main.hwnd, WM_APP_PROGRESS, 0, -1);
        if (!game::ensureMinecraft(inst.mcVersion, &error, postProgress, &g_main.cancelled, &assetIndexId)) {
            logTelemetry("ensureMinecraft fail " + error);
            postJobDone(new JobDone{JobDone::Kind::Launch, false, error, ""});
            return;
        }
        {
            std::wstring idir = instanceDir(fromUtf8(inst.name));
            InstanceCfg cur = loadInstance(idir);
            cur.assetIndexId = assetIndexId;
            saveInstance(idir, cur);
        }
        if (fabric) {
            postStatus(std::string(lang("checking_fabric")) + inst.mcVersion + "...");
            PostMessageW(g_main.hwnd, WM_APP_PROGRESS, 0, -1);
            if (!game::ensureFabric(inst.mcVersion, inst.loaderVersion, &error, postProgress, &g_main.cancelled)) {
                logTelemetry("ensureFabric fail " + error);
                postJobDone(new JobDone{JobDone::Kind::Launch, false, error, ""});
                return;
            }
        }
    } else {
        assetIndexId = inst.assetIndexId;
        if (assetIndexId.empty()) assetIndexId = game::getFallbackAssetIndexId(inst.mcVersion);
    }
    logTelemetry("launch assetIndex=" + assetIndexId + " installed=" + std::string(installed ? "1" : "0"));
    postStatus(std::string(lang("checking_integrity")));
    if (!game::quickIntegrityCheck(inst.mcVersion, assetIndexId, &error)) {
        logTelemetry("integrity fail " + error);
        postJobDone(new JobDone{JobDone::Kind::Launch, false, error, ""});
        return;
    }
    logTelemetry("integrity ok");
    game::LaunchParams params;
    params.username = account.name;
    params.uuid = account.uuid;
    params.accessToken = account.accessToken;
    params.mcVersion = inst.mcVersion;
    params.gameDir = instanceDir(fromUtf8(inst.name));
    params.ramMb = inst.ramMb;
    params.jvmArgs = inst.jvmArgs;
    params.fabric = fabric;
    params.javaExe = javaExe;
    params.offline = inst.offlineMode;
    params.assetIndexId = assetIndexId;
    if (!params.offline && !installed) {
        postStatus(std::string(lang("checking_network")));
        if (!isOnline()) {
            postStatus(std::string(lang("no_internet")));
            params.offline = true;
            params.accessToken = "";
        }
    }
    postStatus(std::string(lang("launching")));
    logTelemetry("launchMinecraft try mc=" + params.mcVersion);
    if (!game::launchMinecraft(params, &error, postConsoleLine, g_gameProc)) {
        logTelemetry("launchMinecraft fail " + error);
        postJobDone(new JobDone{JobDone::Kind::Launch, false, error, ""});
        return;
    }
    logTelemetry("launchMinecraft success");
    postJobDone(new JobDone{JobDone::Kind::Launch, true, "", ""});
    PostMessageW(g_main.hwnd, WM_APP_GAME_RUNNING, 0, 0);
    while (g_gameProc.isRunning() && !g_killRequested) Sleep(400);
    if (g_killRequested) g_gameProc.kill();
    while (g_gameProc.isRunning()) Sleep(100);
    g_gameProc.close();
    logTelemetry("game exited");
    PostMessageW(g_main.hwnd, WM_APP_GAME_EXITED, 0, 0);
}

void workerOffline(const std::string& name) {
    AccountDone* done = new AccountDone();
    done->ok = false;
    if (game::createOfflineAccount(name, &done->account)) {
        done->ok = true;
    } else {
        done->error = std::string(lang("offline_account_failed"));
    }
    postAccountDone(done);
}

void onLaunch() {
    if (g_main.busy || g_main.gameRunning) return;
    int idx = selectedInstanceIndex();
    if (idx < 0) {
        setStatus(std::string(lang("select_instance_first")));
        return;
    }
    int accIdx = static_cast<int>(SendMessageW(g_main.hAccounts, CB_GETCURSEL, 0, 0));
    if (accIdx < 0 || accIdx >= static_cast<int>(g_main.cfg.accounts.size())) {
        setStatus(std::string(lang("no_account_selected")));
        return;
    }
    InstanceCfg inst = g_main.filteredInstances[idx];
    Account account = g_main.cfg.accounts[accIdx];
    std::wstring logDir = joinPath(instanceDir(fromUtf8(inst.name)), L"logs");
    createDirs(logDir);
    SYSTEMTIME st; GetLocalTime(&st);
    wchar_t logName[128];
    _snwprintf_s(logName, 128, L"launch_%04d%02d%02d_%02d%02d%02d.log",
                 st.wYear, st.wMonth, st.wDay, st.wHour, st.wMinute, st.wSecond);
    std::wstring logFile = joinPath(logDir, logName);
    openConsole([] {
        g_killRequested = true;
    }, logFile, g_main.cfg.saveLogs);
    setBusy(true);
    std::thread(workerLaunch, inst, account).detach();
}

void onUpdate() {
    if (g_main.busy) return;
    setBusy(true);
    std::thread(workerUpdate).detach();
}

void onConsole() {
    openConsole([] {
        g_killRequested = true;
    });
}

void onAddInstance() {
    if (g_main.busy) return;
    InstanceCfg cfg;
    cfg.name = "NewInstance";
    cfg.mcVersion = "1.21.11";
    cfg.ramMb = 4096;
    cfg.loader = "vanilla";
    if (!showInstanceEditor(g_main.hwnd, cfg, true)) return;
    std::wstring dir = instanceDir(fromUtf8(cfg.name));
    if (fileExists(joinPath(dir, L"instance.cfg"))) {
        MessageBoxW(g_main.hwnd, fromUtf8(lang("instance_exists")).c_str(), L"KickX Launcher",
                    MB_ICONWARNING | MB_OK);
        return;
    }
    createDirs(joinPath(dir, L"mods"));
    saveInstance(dir, cfg);
    refreshInstances();
    setStatus(std::string(lang("instance_created")) + cfg.name);
}

void onEditInstance() {
    if (g_main.busy) return;
    int idx = selectedInstanceIndex();
    if (idx < 0) {
        setStatus(std::string(lang("select_instance_first")));
        return;
    }
    InstanceCfg cfg = g_main.filteredInstances[idx];
    if (!showInstanceEditor(g_main.hwnd, cfg, false)) return;
    std::wstring oldDir = instanceDir(fromUtf8(g_main.filteredInstances[idx].name));
    std::wstring newDir = instanceDir(fromUtf8(cfg.name));
    if (cfg.name != g_main.filteredInstances[idx].name) {
        if (fileExists(joinPath(newDir, L"instance.cfg"))) {
            MessageBoxW(g_main.hwnd, fromUtf8(lang("instance_exists")).c_str(), L"KickX Launcher",
                        MB_ICONWARNING | MB_OK);
            return;
        }
        if (fileExists(oldDir)) MoveFileW(oldDir.c_str(), newDir.c_str());
    }
    saveInstance(newDir, cfg);
    refreshInstances();
    setStatus(std::string(lang("instance_saved")) + cfg.name);
}

void onDeleteInstance() {
    if (g_main.busy) return;
    int idx = static_cast<int>(SendMessageW(g_main.hList, LB_GETCURSEL, 0, 0));
    if (idx < 0 || idx >= static_cast<int>(g_main.filteredInstances.size())) {
        setStatus(std::string(lang("select_instance_first")));
        return;
    }
    InstanceCfg inst = g_main.filteredInstances[idx];
    std::wstring dir = instanceDir(fromUtf8(inst.name));
    std::wstring msg = fromUtf8(lang("delete_confirm")) + L" \"" + fromUtf8(inst.name) + L"\"" + fromUtf8(lang("delete_confirm_files"));
    if (MessageBoxW(g_main.hwnd, msg.c_str(), L"KickX Launcher", MB_ICONWARNING | MB_YESNO) != IDYES) return;
    deleteInstance(dir);
    refreshInstances();
    setStatus(std::string(lang("instance_deleted")));
}

void onAddOffline() {
    if (g_main.busy) return;
    std::string name;
    if (!showOfflineAccountDialog(g_main.hwnd, name)) return;
    if (name.empty()) return;
    setBusy(true);
    std::thread(workerOffline, name).detach();
}

void onAddMs() {
    if (g_main.busy) return;
    Account acc;
    std::string err;
    if (!showMicrosoftAccountDialog(g_main.hwnd, &acc, &err)) {
        return;
    }
    AccountDone* done = new AccountDone();
    done->ok = true;
    done->account = acc;
    postAccountDone(done);
}

void doLayout() {
    if (!g_main.hwnd) return;
    RECT rc;
    GetClientRect(g_main.hwnd, &rc);
    int w = rc.right - rc.left;
    int h = rc.bottom - rc.top;
    int pad = 16;
    int btnW = 128;
    int listRight = w - pad - btnW - 12;
    int searchH = 28;
    int searchTop = 28;
    int listTop = searchTop + searchH + 8;
    int accountsY = h - 162;
    if (accountsY < listTop + 140) accountsY = listTop + 140;
    int listH = accountsY - listTop - 12;
    if (listH < 120) listH = 120;
    int statusY = accountsY + 36;
    int progressY = statusY + 30;
    int bottomY = h - 44;
    HDWP dwp = BeginDeferWindowPos(17);
    if (dwp) {
        auto place = [&](HWND hwnd, int x, int y, int cw, int ch) {
            DeferWindowPos(dwp, hwnd, nullptr, x, y, cw, ch, SWP_NOZORDER);
        };
        place(g_main.hSearch, pad, searchTop, listRight - pad - 160 - 8, searchH);
        place(g_main.hSort, listRight - 160, searchTop, 160, 200);
        place(g_main.hList, pad, listTop, listRight - pad, listH);
        place(g_main.hAdd, w - pad - btnW, listTop, btnW, 34);
        place(g_main.hEdit, w - pad - btnW, listTop + 44, btnW, 34);
        place(g_main.hDel, w - pad - btnW, listTop + 88, btnW, 34);
        place(g_main.hSettings, w - pad - 36, 0, 36, 26);
        place(g_main.hAccounts, pad, accountsY, 200, 220);
        place(g_main.hOffline, pad + 232, accountsY, 140, 28);
        place(g_main.hMs, pad + 380, accountsY, 170, 28);
        place(g_main.hSkin, w - pad - 72, accountsY - 8, 72, 72);
        place(g_main.hStatus, pad, statusY, w - pad * 2, 24);
        place(g_main.hProgress, pad, progressY, w - pad * 2, 18);
        place(g_main.hLaunch, pad, bottomY, 130, 36);
        place(g_main.hUpdate, pad + 142, bottomY, 148, 36);
        place(g_main.hConsoleBtn, pad + 302, bottomY, 108, 36);
        place(g_main.hWinLabel, w - pad - 150, h - 20, 150, 16);
        EndDeferWindowPos(dwp);
    }
}

void setBtnIcon(HWND btn, const std::wstring& name);

void onCreate(HWND hwnd) {
    INITCOMMONCONTROLSEX icc{};
    icc.dwSize = sizeof(icc);
    icc.dwICC = ICC_WIN95_CLASSES | ICC_COOL_CLASSES | ICC_USEREX_CLASSES | ICC_PROGRESS_CLASS;
    InitCommonControlsEx(&icc);
    g_main.hwnd = hwnd;
    g_main.cfg = loadLauncherConfig();
    setCurrentLanguage(g_main.cfg.language.c_str());
    if (!g_main.font) g_main.font = makeFont();
    if (!g_main.iconFont) g_main.iconFont = makeIconFont();
    g_main.hList = CreateWindowExW(0, L"LISTBOX", nullptr,
                                   WS_CHILD | WS_VISIBLE | WS_BORDER | WS_VSCROLL | LBS_NOTIFY | LBS_NOINTEGRALHEIGHT,
                                   0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_INSTANCES),
                                   GetModuleHandleW(nullptr), nullptr);
    g_main.hSearch = CreateWindowExW(0, L"EDIT", L"",
                                     WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
                                     0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_SEARCH_INST),
                                     GetModuleHandleW(nullptr), nullptr);
    SendMessageW(g_main.hSearch, EM_SETCUEBANNER, TRUE, reinterpret_cast<LPARAM>(fromUtf8(lang("search_instances")).c_str()));
    g_main.hSort = CreateWindowExW(0, L"COMBOBOX", nullptr,
                                   WS_CHILD | WS_VISIBLE | WS_VSCROLL | CBS_DROPDOWNLIST,
                                   0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_SORT_INST),
                                   GetModuleHandleW(nullptr), nullptr);
    SendMessageW(g_main.hSort, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(lang("sort_name_az")).c_str()));
    SendMessageW(g_main.hSort, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(lang("sort_name_za")).c_str()));
    SendMessageW(g_main.hSort, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(lang("sort_version")).c_str()));
    SendMessageW(g_main.hSort, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(lang("sort_date")).c_str()));
    SendMessageW(g_main.hSort, CB_SETCURSEL, 0, 0);
    g_main.hAdd = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("add")).c_str(),
                                   WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                   0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_ADD_INST),
                                   GetModuleHandleW(nullptr), nullptr);
    g_main.hEdit = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("edit")).c_str(),
                                    WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                    0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_EDIT_INST),
                                    GetModuleHandleW(nullptr), nullptr);
    g_main.hDel = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("delete")).c_str(),
                                   WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                   0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_DEL_INST),
                                   GetModuleHandleW(nullptr), nullptr);
    g_main.hSettings = CreateWindowExW(0, L"BUTTON", L"",
                                    WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                    0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_SETTINGS),
                                    GetModuleHandleW(nullptr), nullptr);
    g_main.hAccounts = CreateWindowExW(0, L"COMBOBOX", nullptr,
                                        WS_CHILD | WS_VISIBLE | WS_VSCROLL | CBS_DROPDOWNLIST | CBS_OWNERDRAWFIXED | CBS_HASSTRINGS,
                                        0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_ACCOUNTS),
                                        GetModuleHandleW(nullptr), nullptr);
    SendMessageW(g_main.hAccounts, WM_SETFONT, reinterpret_cast<WPARAM>(g_main.font), TRUE);
    g_main.hOffline = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("offline")).c_str(),
                                       WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                       0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_ADD_OFFLINE),
                                       GetModuleHandleW(nullptr), nullptr);
    g_main.hMs = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("microsoft")).c_str(),
                                  WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                  0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_ADD_MS),
                                  GetModuleHandleW(nullptr), nullptr);
    g_main.hAccountsIcon = nullptr;
    g_main.hStatus = CreateWindowExW(0, L"STATIC", L"",
                                      WS_CHILD | WS_VISIBLE | SS_LEFT,
                                      0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_STATUS),
                                      GetModuleHandleW(nullptr), nullptr);
    g_main.hProgress = CreateWindowExW(0, PROGRESS_CLASSW, nullptr,
                                        WS_CHILD | WS_VISIBLE,
                                        0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_PROGRESS),
                                        GetModuleHandleW(nullptr), nullptr);
    g_main.hLaunch = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("launch")).c_str(),
                                      WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                      0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_LAUNCH),
                                      GetModuleHandleW(nullptr), nullptr);
    g_main.hUpdate = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("check_update")).c_str(),
                                      WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                      0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_UPDATE),
                                      GetModuleHandleW(nullptr), nullptr);
    g_main.hConsoleBtn = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("console")).c_str(),
                                          WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                          0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_CONSOLE),
                                          GetModuleHandleW(nullptr), nullptr);
    g_main.hWinLabel = CreateWindowExW(0, L"STATIC", fromUtf8(lang("for_windows")).c_str(),
                                       WS_CHILD | WS_VISIBLE | SS_OWNERDRAW,
                                       0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_WINLABEL),
                                       GetModuleHandleW(nullptr), nullptr);
    g_main.hSkin = CreateWindowExW(0, L"STATIC", nullptr,
                                   WS_CHILD | WS_VISIBLE | SS_OWNERDRAW | WS_BORDER,
                                   0, 0, 10, 10, hwnd, reinterpret_cast<HMENU>(IDC_SKIN_PREVIEW),
                                   GetModuleHandleW(nullptr), nullptr);
    for (HWND child : {g_main.hList, g_main.hSearch, g_main.hSort, g_main.hAccounts, g_main.hStatus, g_main.hProgress, g_main.hOffline, g_main.hMs, g_main.hLaunch, g_main.hUpdate, g_main.hConsoleBtn, g_main.hWinLabel, g_main.hSkin}) {
        SendMessageW(child, WM_SETFONT, reinterpret_cast<WPARAM>(g_main.font), TRUE);
    }
    for (HWND child : {g_main.hAdd, g_main.hEdit, g_main.hDel, g_main.hSettings}) {
        SendMessageW(child, WM_SETFONT, reinterpret_cast<WPARAM>(g_main.iconFont), TRUE);
    }
    doLayout();
    g_main.theme = getThemeForConfig(g_main.cfg.theme, g_main.cfg.customBg, g_main.cfg.customPanel, g_main.cfg.customText, g_main.cfg.customAccent, g_main.cfg.customButton, g_main.cfg.customAlpha);
    applyWindowTheme(hwnd, g_main.theme);
    setBtnIcon(g_main.hLaunch, L"play");
    setBtnIcon(g_main.hUpdate, L"update");
    setBtnIcon(g_main.hConsoleBtn, L"console");
    setBtnIcon(g_main.hAdd, L"add");
    setBtnIcon(g_main.hEdit, L"edit");
    setBtnIcon(g_main.hDel, L"delete");
    setBtnIcon(g_main.hSettings, L"settings");
    setBtnIcon(g_main.hOffline, L"offline");
    setBtnIcon(g_main.hMs, L"microsoft");
    g_main.acctIconBmp = loadIconBmp(iconPath(L"accounts"), 16);
    if(g_main.acctIconBmp && g_main.acctIconBmp->GetHICON(&g_main.acctIcon) != Gdiplus::Ok) g_main.acctIcon = nullptr;
    g_main.winLabelBmp = loadIconBmp(iconPath(L"win10"), 16);
    g_main.glow.hGlow = nullptr;
    g_main.glow.bmp = nullptr;
    createGlowBitmap();
    glowSetButtons(&g_main.glow, {g_main.hLaunch, g_main.hUpdate, g_main.hConsoleBtn, g_main.hAdd, g_main.hEdit, g_main.hDel, g_main.hSettings, g_main.hOffline, g_main.hMs});
    SetTimer(g_main.hwnd, 2, 16, nullptr);
    setBusy(false);
    {
        const char* v=lang("ready"); if(!v||strcmp(v,"ready")==0) v="Ready";
        setStatus(v);
    }
    refreshAccounts();
    refreshInstances();
    if (g_main.cfg.accounts.empty()) {
        const char* v=lang("add_account_hint"); if(!v||strcmp(v,"add_account_hint")==0) v="Добавь аккаунт для игры";
        setStatus(v);
    }
}

void onJobDone(JobDone* done) {
    setBusy(false);
    if (done->ok) {
        switch (done->kind) {
            case JobDone::Kind::Update:
                refreshInstances();
                setStatus(std::string(lang("ravex_updated")) + done->extra);
                break;
            case JobDone::Kind::Launch:
                setStatus(std::string(lang("game_launched")));
                break;
        }
    } else {
        if (g_main.cancelled) {
            setStatus(std::string(lang("cancelled")));
        } else {
            setStatus(std::string(lang("error_prefix")) + (done->error.empty() ? std::string("unknown") : done->error));
        }
    }
    delete done;
}

void onAccountDone(AccountDone* done) {
    setBusy(false);
    if (done->ok) {
        g_main.cfg.accounts.push_back(done->account);
        g_main.cfg.activeAccount = static_cast<int>(g_main.cfg.accounts.size()) - 1;
        saveLauncherConfig(g_main.cfg);
        refreshAccounts();
        setStatus(std::string(lang("account_added")) + done->account.name);
    } else {
        setStatus(std::string(lang("login_failed")) + (done->error.empty() ? std::string("unknown") : done->error));
    }
    delete done;
}

void setBtnIcon(HWND btn, const std::wstring& name) {
    std::wstring p = iconPath(name);
    bool iconOnly = GetWindowTextLengthW(btn) == 0;
    auto fallback = [&](){
        std::wstring wtxt = L"+";
        bool isSettings = (name == L"settings");
        if (name == L"add") wtxt = fromUtf8(lang("add"));
        else if (name == L"edit") wtxt = fromUtf8(lang("edit"));
        else if (name == L"delete") wtxt = fromUtf8(lang("delete"));
        else if (isSettings) wtxt = L"\xE713";
        else if (name == L"play") wtxt = fromUtf8(lang("launch"));
        else if (name == L"update") wtxt = fromUtf8(lang("check_update"));
        else if (name == L"console") wtxt = fromUtf8(lang("console"));
        SetWindowTextW(btn, wtxt.c_str());
        LONG_PTR st = GetWindowLongPtrW(btn, GWL_STYLE);
        st = (st & ~BS_TYPEMASK) | BS_PUSHBUTTON;
        SetWindowLongPtrW(btn, GWL_STYLE, st);
        SendMessageW(btn, WM_SETFONT, (WPARAM)(isSettings ? g_main.iconFont : g_main.font), TRUE);
        InvalidateRect(btn, nullptr, TRUE);
    };
    if (!fileExists(p)) { if (iconOnly) fallback(); return; }
    int sz = iconOnly ? 20 : 18;
    Gdiplus::Bitmap* bmp = loadIconBmp(p, sz);
    if (!bmp) { if (iconOnly) fallback(); return; }
    LONG_PTR st = GetWindowLongPtrW(btn, GWL_STYLE);
    bool isOwnerDraw = (st & BS_TYPEMASK) == BS_OWNERDRAW;
    if (isOwnerDraw) {
        auto it=g_main.btnBmp.find(btn);
        if(it!=g_main.btnBmp.end() && it->second) delete it->second;
        g_main.btnBmp[btn]=bmp;
        g_main.iconBmps.push_back(bmp);
        bool found=false;
        for(HWND b:g_main.ownerBtns) if(b==btn){found=true;break;}
        if(!found) g_main.ownerBtns.push_back(btn);
        InvalidateRect(btn,nullptr,TRUE);
        return;
    }
    HICON hIc = nullptr;
    bmp->GetHICON(&hIc);
    delete bmp;
    if (iconOnly) {
        st = (st & ~BS_TYPEMASK) | BS_ICON;
        SetWindowLongPtrW(btn, GWL_STYLE, st);
        SendMessageW(btn, BM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIc));
    } else {
        HIMAGELIST hil = ImageList_Create(sz, sz, ILC_COLOR32 | ILC_MASK, 1, 1);
        ImageList_AddIcon(hil, hIc);
        DestroyIcon(hIc);
        BUTTON_IMAGELIST bi{};
        bi.himl = hil;
        bi.uAlign = BUTTON_IMAGELIST_ALIGN_LEFT;
        bi.margin.left = 6;
        bi.margin.right = 4;
        SendMessageW(btn, BCM_SETIMAGELIST, 0, reinterpret_cast<LPARAM>(&bi));
    }
}

LRESULT CALLBACK MainProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    switch (msg) {
        case WM_CREATE:
            onCreate(hwnd);
            return 0;
        case WM_SIZE:
            doLayout();
            return 0;
        case WM_GETMINMAXINFO: {
            MINMAXINFO* mi = reinterpret_cast<MINMAXINFO*>(lParam);
            mi->ptMinTrackSize.x = 720;
            mi->ptMinTrackSize.y = 520;
            return 0;
        }
        case WM_TIMER:
            if (wParam == 2) {
                moveGlowToCursor();
                return 0;
            }
            if (wParam == 1 && g_main.busy) {
                static int dots = 0;
                dots = (dots + 1) % 4;
                std::wstring status;
                int len = GetWindowTextLengthW(g_main.hStatus);
                status.resize(len);
                GetWindowTextW(g_main.hStatus, status.data(), len + 1);
                std::string cur = toUtf8(status);
                while (!cur.empty() && cur.back() == '.') cur.pop_back();
                for (int i = 0; i < dots; ++i) cur += ".";
                SetWindowTextW(g_main.hStatus, fromUtf8(cur).c_str());
            }
            return 0;
        case WM_COMMAND:
            switch (LOWORD(wParam)) {
                case IDC_LAUNCH:
                    onLaunch();
                    break;
                case IDC_UPDATE:
                    onUpdate();
                    break;
                case IDC_CONSOLE:
                    onConsole();
                    break;
                case IDC_ADD_INST:
                    onAddInstance();
                    break;
                case IDC_EDIT_INST:
                    onEditInstance();
                    break;
                case IDC_DEL_INST:
                    onDeleteInstance();
                    break;
                case IDC_SETTINGS:
                    if (showSettingsDialog(g_main.hwnd)) {
                        g_main.cfg = loadLauncherConfig();
                        setCurrentLanguage(g_main.cfg.language.c_str());
                        g_main.theme = getThemeForConfig(g_main.cfg.theme, g_main.cfg.customBg, g_main.cfg.customPanel, g_main.cfg.customText, g_main.cfg.customAccent, g_main.cfg.customButton, g_main.cfg.customAlpha);
                        if (g_main.bgBrush) DeleteObject(g_main.bgBrush);
                        g_main.bgBrush = CreateSolidBrush(g_main.theme.bg);
                        if (g_main.panelBrush) DeleteObject(g_main.panelBrush);
                        g_main.panelBrush = CreateSolidBrush(g_main.theme.panel);
                        applyWindowTheme(g_main.hwnd, g_main.theme);
                        SetWindowTextW(g_main.hLaunch, fromUtf8(lang("launch")).c_str());
                        SetWindowTextW(g_main.hUpdate, fromUtf8(lang("check_update")).c_str());
                        SetWindowTextW(g_main.hConsoleBtn, fromUtf8(lang("console")).c_str());
                        SetWindowTextW(g_main.hOffline, fromUtf8(lang("offline")).c_str());
                        SetWindowTextW(g_main.hMs, fromUtf8(lang("microsoft")).c_str());
                        SetWindowTextW(g_main.hAdd, fromUtf8(lang("add")).c_str());
                        SetWindowTextW(g_main.hEdit, fromUtf8(lang("edit")).c_str());
                        SetWindowTextW(g_main.hDel, fromUtf8(lang("delete")).c_str());
                        SetWindowTextW(g_main.hWinLabel, fromUtf8(lang("for_windows")).c_str());
                        std::wstring mainTitle = L"KickX Launcher v1.1 - " + fromUtf8(lang("for_windows"));
                        SetWindowTextW(g_main.hwnd, mainTitle.c_str());
                        SendMessageW(g_main.hSearch, EM_SETCUEBANNER, TRUE, reinterpret_cast<LPARAM>(fromUtf8(lang("search_instances")).c_str()));
                        {
                            int cur = static_cast<int>(SendMessageW(g_main.hSort, CB_GETCURSEL, 0, 0));
                            SendMessageW(g_main.hSort, CB_RESETCONTENT, 0, 0);
                            SendMessageW(g_main.hSort, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(lang("sort_name_az")).c_str()));
                            SendMessageW(g_main.hSort, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(lang("sort_name_za")).c_str()));
                            SendMessageW(g_main.hSort, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(lang("sort_version")).c_str()));
                            SendMessageW(g_main.hSort, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(lang("sort_date")).c_str()));
                            if (cur >=0 && cur <4) SendMessageW(g_main.hSort, CB_SETCURSEL, cur, 0); else SendMessageW(g_main.hSort, CB_SETCURSEL, g_main.sortMode, 0);
                        }
                        RedrawWindow(g_main.hwnd, nullptr, nullptr, RDW_INVALIDATE | RDW_ALLCHILDREN | RDW_UPDATENOW | RDW_FRAME);
                        refreshInstances();
                        refreshAccounts();
                        setStatus(lang("ready") ? lang("ready") : "Ready");
                    }
                    break;
                case IDC_ADD_OFFLINE:
                    onAddOffline();
                    break;
                case IDC_ADD_MS:
                    onAddMs();
                    break;
            }
            if (HIWORD(wParam) == CBN_SELCHANGE && LOWORD(wParam) == IDC_ACCOUNTS) {
                int sel = static_cast<int>(SendMessageW(g_main.hAccounts, CB_GETCURSEL, 0, 0));
                if (sel >= 0 && sel < static_cast<int>(g_main.cfg.accounts.size())) {
                    g_main.cfg.activeAccount = sel;
                    saveLauncherConfig(g_main.cfg);
                    updateSkinPreview();
                }
            }
            if (HIWORD(wParam) == EN_CHANGE && LOWORD(wParam) == IDC_SEARCH_INST) {
                int len = GetWindowTextLengthW(g_main.hSearch);
                std::wstring txt; txt.resize(len+1); GetWindowTextW(g_main.hSearch, txt.data(), len+1); if(len>0) txt.resize(len); else txt.clear();
                g_main.searchFilter = txt;
                refreshInstances();
            }
            if (HIWORD(wParam) == CBN_SELCHANGE && LOWORD(wParam) == IDC_SORT_INST) {
                g_main.sortMode = static_cast<int>(SendMessageW(g_main.hSort, CB_GETCURSEL, 0, 0));
                refreshInstances();
            }
            return 0;
        case WM_MEASUREITEM: {
            if (!lParam) return FALSE;
            MEASUREITEMSTRUCT* mis = reinterpret_cast<MEASUREITEMSTRUCT*>(lParam);
            if (mis->CtlID == IDC_ACCOUNTS) { mis->itemHeight = 24; return TRUE; }
            return FALSE;
        }
        case WM_DRAWITEM: {
            if (!lParam) return FALSE;
            DRAWITEMSTRUCT* ds=(DRAWITEMSTRUCT*)lParam;
            if (!ds->hDC) return FALSE;
            int id=(int)ds->CtlID;
            bool isMainBtn = (id==IDC_ADD_INST||id==IDC_EDIT_INST||id==IDC_DEL_INST||id==IDC_SETTINGS||id==IDC_ADD_OFFLINE||id==IDC_ADD_MS||id==IDC_LAUNCH||id==IDC_UPDATE||id==IDC_CONSOLE);
            if(isMainBtn){
                HDC dc=ds->hDC; RECT rc=ds->rcItem;
                bool isPrimary = (id==IDC_LAUNCH);
                bool isSmallIcon = (id==IDC_ADD_INST||id==IDC_EDIT_INST||id==IDC_DEL_INST||id==IDC_SETTINGS);
                COLORREF bg = isPrimary ? g_main.theme.accent : RGB(45,45,47);
                COLORREF fg = RGB(255,255,255);
                if(ds->itemState & ODS_DISABLED){ bg=RGB(60,60,60); fg=RGB(130,130,130); }
                else if(ds->itemState & ODS_SELECTED){ bg=isPrimary?RGB(70,120,235):RGB(60,60,62); }
                else if(ds->itemState & ODS_HOTLIGHT){ bg=isPrimary?RGB(100,150,255):RGB(55,55,57); }
                Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                HBRUSH bgFill=CreateSolidBrush(bg); FillRect(dc,&rc,bgFill); DeleteObject(bgFill);
                Gdiplus::Color gc(GetRValue(bg),GetGValue(bg),GetBValue(bg)); Gdiplus::SolidBrush br(gc);
                Gdiplus::GraphicsPath path; path.AddArc(rc.left,rc.top,8,8,180,90); path.AddArc(rc.right-8,rc.top,8,8,270,90); path.AddArc(rc.right-8,rc.bottom-8,8,8,0,90); path.AddArc(rc.left,rc.bottom-8,8,8,90,90); path.CloseFigure();
                g.FillPath(&br,&path);
                HWND btn=ds->hwndItem;
                Gdiplus::Bitmap* bmp=nullptr;
                auto it=g_main.btnBmp.find(btn);
                if(it!=g_main.btnBmp.end()) bmp=it->second;
                int iconSz= isSmallIcon?16:18;
                int textLeft=rc.left;
                if(bmp){
                    int ix=rc.left+8;
                    int iy=rc.top+(rc.bottom-rc.top-iconSz)/2;
                    g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
                    g.DrawImage(bmp, ix, iy, iconSz, iconSz);
                    textLeft=ix+iconSz+6;
                } else textLeft=rc.left+8;
                wchar_t txt[64]; GetWindowTextW(btn,txt,64);
                if(wcslen(txt)>0 && g_main.font){
                    Gdiplus::SolidBrush tbr(Gdiplus::Color(GetRValue(fg),GetGValue(fg),GetBValue(fg)));
                    Gdiplus::Font f(dc,g_main.font);
                    Gdiplus::StringFormat fmt; fmt.SetAlignment(isSmallIcon?Gdiplus::StringAlignmentCenter:Gdiplus::StringAlignmentNear); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter); fmt.SetTrimming(Gdiplus::StringTrimmingEllipsisCharacter);
                    Gdiplus::RectF rf((Gdiplus::REAL)textLeft,(Gdiplus::REAL)rc.top,(Gdiplus::REAL)(rc.right-textLeft-4),(Gdiplus::REAL)(rc.bottom-rc.top));
                    if(isSmallIcon) rf=Gdiplus::RectF((Gdiplus::REAL)rc.left,(Gdiplus::REAL)rc.top,(Gdiplus::REAL)(rc.right-rc.left),(Gdiplus::REAL)(rc.bottom-rc.top));
                    g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                    g.DrawString(txt,-1,&f,rf,&fmt,&tbr);
                }
                return TRUE;
            }
            if(id==IDC_ACCOUNTS){
                HDC dc=ds->hDC; RECT rc=ds->rcItem;
                bool isEdit=(ds->itemState & ODS_COMBOBOXEDIT)!=0;
                COLORREF bg=g_main.theme.panel;
                HBRUSH br=CreateSolidBrush(bg); FillRect(dc,&rc,br); DeleteObject(br);
                SetBkColor(dc,bg); SetTextColor(dc,RGB(255,255,255));
                int iconSz=16, pad=6;
                int ix=rc.left+pad, iy=rc.top+(rc.bottom-rc.top-iconSz)/2;
                if(g_main.acctIcon){
                    DrawIconEx(dc,ix,iy,g_main.acctIcon,iconSz,iconSz,0,nullptr,DI_NORMAL);
                }
                RECT tr=rc; tr.left=ix+iconSz+pad; tr.right-=pad;
                wchar_t buf[256]={};
                if(isEdit){ if(ds->hwndItem) GetWindowTextW(ds->hwndItem,buf,256); }
                else if(ds->itemID!=(UINT)-1 && ds->hwndItem){ int len=(int)SendMessageW(ds->hwndItem,CB_GETLBTEXTLEN,ds->itemID,0); if(len>0 && len<256){ SendMessageW(ds->hwndItem,CB_GETLBTEXT,ds->itemID,reinterpret_cast<LPARAM>(buf)); } else if(len==0){ SendMessageW(ds->hwndItem,CB_GETLBTEXT,ds->itemID,reinterpret_cast<LPARAM>(buf)); } }
                if(buf[0] && g_main.font){
                    HFONT oldF=(HFONT)SelectObject(dc,g_main.font);
                    SetBkMode(dc,TRANSPARENT);
                    DrawTextW(dc,buf,-1,&tr,DT_SINGLELINE|DT_VCENTER|DT_LEFT|DT_END_ELLIPSIS);
                    SelectObject(dc,oldF);
                }
                if(isEdit && (ds->itemState & ODS_FOCUS)){
                    RECT fr=rc; fr.left+=iconSz+pad*2; DrawFocusRect(dc,&fr);
                }
                return TRUE;
            }
            if(id==IDC_WINLABEL){
                HDC dc=ds->hDC; RECT rc=ds->rcItem;
                HBRUSH bgBr=CreateSolidBrush(g_main.theme.bg); FillRect(dc,&rc,bgBr); DeleteObject(bgBr);
                SetBkColor(dc,g_main.theme.bg); SetTextColor(dc,RGB(150,150,150));
                SetBkMode(dc,TRANSPARENT);
                int iconSz=16, pad=4;
                wchar_t txt[128]; GetWindowTextW(ds->hwndItem,txt,128);
                HFONT oldF=(HFONT)SelectObject(dc,g_main.font);
                SIZE sz{}; GetTextExtentPoint32W(dc,txt,wcslen(txt),&sz);
                int totalW=sz.cx; if(g_main.winLabelBmp) totalW+=iconSz+pad;
                int ix=rc.right-totalW;
                int iy=rc.top+(rc.bottom-rc.top-iconSz)/2;
                if(g_main.winLabelBmp){
                    Gdiplus::Graphics g(dc); g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
                    g.DrawImage(g_main.winLabelBmp, ix, iy, iconSz, iconSz);
                    ix+=iconSz+pad;
                }
                RECT tr={ix,rc.top,rc.right,rc.bottom};
                DrawTextW(dc,txt,-1,&tr,DT_LEFT|DT_VCENTER|DT_SINGLELINE|DT_END_ELLIPSIS);
                SelectObject(dc,oldF);
                return TRUE;
            }
            if(id==IDC_SKIN_PREVIEW){
                HDC dc=ds->hDC; RECT rc=ds->rcItem;
                HBRUSH bgBr=CreateSolidBrush(g_main.theme.panel); FillRect(dc,&rc,bgBr); DeleteObject(bgBr);
                if(g_main.skinBmp){
                    Gdiplus::Graphics g(dc);
                    g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
                    g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                    int pad=4;
                    Gdiplus::Rect dst(rc.left+pad, rc.top+pad, rc.right-rc.left-pad*2, rc.bottom-rc.top-pad*2);
                    g.DrawImage(g_main.skinBmp, dst);
                } else {
                    SetBkMode(dc, TRANSPARENT);
                    SetTextColor(dc, RGB(150,150,150));
                    HFONT oldF=(HFONT)SelectObject(dc, g_main.font);
                    RECT tr=rc; tr.top+= (rc.bottom-rc.top)/2 - 8;
                    DrawTextW(dc, L"Skin", -1, &tr, DT_CENTER|DT_SINGLELINE|DT_END_ELLIPSIS);
                    SelectObject(dc, oldF);
                }
                HPEN pen=CreatePen(PS_SOLID, 1, RGB(60,60,60));
                HPEN oldP=(HPEN)SelectObject(dc, pen);
                HBRUSH oldB=(HBRUSH)SelectObject(dc, GetStockObject(NULL_BRUSH));
                Rectangle(dc, rc.left, rc.top, rc.right, rc.bottom);
                SelectObject(dc, oldP); SelectObject(dc, oldB); DeleteObject(pen);
                return TRUE;
            }
            return FALSE;
        }
        case WM_CTLCOLORSTATIC:
        case WM_CTLCOLORBTN: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            HWND ctrl = reinterpret_cast<HWND>(lParam);
            if (ctrl == g_main.hAccounts) {
                SetBkColor(dc, g_main.theme.panel);
                SetTextColor(dc, RGB(255,255,255));
                return reinterpret_cast<LRESULT>(g_main.panelBrush);
            }
            SetBkColor(dc, g_main.theme.bg);
            SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(g_main.bgBrush);
        }
        case WM_CTLCOLORLISTBOX:
        case WM_CTLCOLOREDIT: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, g_main.theme.panel);
            SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(g_main.panelBrush);
        }
        case WM_ERASEBKGND: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            RECT rc; GetClientRect(hwnd, &rc);
            FillRect(dc, &rc, g_main.bgBrush);
            return 1;
        }
        case WM_APP_STATUS: {
            std::wstring* payload = reinterpret_cast<std::wstring*>(lParam);
            SetWindowTextW(g_main.hStatus, payload->c_str());
            delete payload;
            return 0;
        }
        case WM_APP_PROGRESS:
            if (lParam < 0) {
                SendMessageW(g_main.hProgress, PBM_SETMARQUEE, TRUE, 0);
            } else {
                SendMessageW(g_main.hProgress, PBM_SETMARQUEE, FALSE, 0);
                SendMessageW(g_main.hProgress, PBM_SETPOS, lParam, 0);
            }
            return 0;
        case WM_APP_JOB_DONE:
            onJobDone(reinterpret_cast<JobDone*>(lParam));
            return 0;
        case WM_APP_CONSOLE_LINE: {
            std::wstring* payload = reinterpret_cast<std::wstring*>(lParam);
            appendConsole(toUtf8(*payload));
            delete payload;
            return 0;
        }
        case WM_APP_ACCOUNT_DONE:
            onAccountDone(reinterpret_cast<AccountDone*>(lParam));
            return 0;
        case WM_APP_GAME_RUNNING:
            g_main.gameRunning = true;
            EnableWindow(g_main.hLaunch, FALSE);
            return 0;
        case WM_APP_GAME_EXITED:
            g_main.gameRunning = false;
            setBusy(false);
            refreshAccounts();
            closeConsole();
            setStatus(std::string(lang("game_exited")));
            return 0;
        case WM_APP_SKIN_DONE: {
            if (lParam) {
                if (g_main.skinBmp) delete g_main.skinBmp;
                g_main.skinBmp = reinterpret_cast<Gdiplus::Bitmap*>(lParam);
            }
            if (g_main.hSkin) { InvalidateRect(g_main.hSkin, nullptr, TRUE); UpdateWindow(g_main.hSkin); }
            return 0;
        }
        case WM_MOUSEMOVE:
            moveGlowToCursor();
            return 0;
        case WM_CLOSE:
            DestroyWindow(hwnd);
            return 0;
        case WM_DESTROY:
            if(g_main.acctIcon) DestroyIcon(g_main.acctIcon);
            delete g_main.acctIconBmp;
            delete g_main.winLabelBmp;
            if(g_main.skinIcon) DestroyIcon(g_main.skinIcon);
            delete g_main.skinBmp;
            glowDestroy(&g_main.glow);
            for(auto& p : g_main.btnBmp) if(p.second) delete p.second;
            g_main.btnBmp.clear();
            g_main.iconBmps.clear();
            if(g_main.font) { DeleteObject(g_main.font); g_main.font = nullptr; }
            if(g_main.iconFont) { DeleteObject(g_main.iconFont); g_main.iconFont = nullptr; }
            if(g_main.bgBrush) { DeleteObject(g_main.bgBrush); g_main.bgBrush = nullptr; }
            if(g_main.panelBrush) { DeleteObject(g_main.panelBrush); g_main.panelBrush = nullptr; }
            PostQuitMessage(0);
            return 0;
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

struct PromptData {
    bool closed = false;
    bool ok = false;
    std::wstring result;
    HWND edit = nullptr;
    HFONT font = nullptr;
};

LRESULT CALLBACK PromptProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    PromptData* data = reinterpret_cast<PromptData*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
    switch (msg) {
        case WM_CREATE: {
            CREATESTRUCTW* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            data = reinterpret_cast<PromptData*>(cs->lpCreateParams);
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(data));
            data->font = makeFont();
            data->edit = CreateWindowExW(0, L"EDIT", L"",
                                         WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
                                         16, 16, 340, 24, hwnd, nullptr, cs->hInstance, nullptr);
            HWND okBtn = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("ok")).c_str(),
                                         WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_DEFPUSHBUTTON,
                                         200, 56, 72, 28, hwnd, reinterpret_cast<HMENU>(IDOK),
                                         cs->hInstance, nullptr);
            HWND cancelBtn = CreateWindowExW(0, L"BUTTON", fromUtf8(lang("cancel")).c_str(),
                                             WS_CHILD | WS_VISIBLE | WS_TABSTOP,
                                             284, 56, 76, 28, hwnd, reinterpret_cast<HMENU>(IDCANCEL),
                                             cs->hInstance, nullptr);
            SendMessageW(data->edit, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            SendMessageW(okBtn, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            SendMessageW(cancelBtn, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            SetFocus(data->edit);
            return 0;
        }
        case WM_COMMAND:
            if (LOWORD(wParam) == IDOK) {
                int len = GetWindowTextLengthW(data->edit);
                data->result.resize(static_cast<std::size_t>(len));
                GetWindowTextW(data->edit, data->result.data(), len + 1);
                data->ok = true;
                data->closed = true;
                DestroyWindow(hwnd);
                return 0;
            }
            if (LOWORD(wParam) == IDCANCEL) {
                data->closed = true;
                DestroyWindow(hwnd);
                return 0;
            }
            return 0;
        case WM_CTLCOLORSTATIC:
        case WM_CTLCOLORBTN: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, g_main.theme.bg);
            SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(GetStockObject(BLACK_BRUSH));
        }
        case WM_CTLCOLOREDIT: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, g_main.theme.panel);
            SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(GetStockObject(BLACK_BRUSH));
        }
        case WM_ERASEBKGND: {
            if (!data) return 0;
            HDC dc = reinterpret_cast<HDC>(wParam);
            RECT rc;
            GetClientRect(hwnd, &rc);
            HBRUSH brush = CreateSolidBrush(g_main.theme.bg);
            FillRect(dc, &rc, brush);
            DeleteObject(brush);
            return 1;
        }
        case WM_CLOSE:
            data->closed = true;
            DestroyWindow(hwnd);
            return 0;
        case WM_DESTROY:
            if (data) data->closed = true;
            DeleteObject(data->font);
            return 0;
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

bool promptForText(HWND parent, const std::wstring& title, std::wstring& out) {
    HINSTANCE inst = reinterpret_cast<HINSTANCE>(GetWindowLongPtrW(parent, GWLP_HINSTANCE));
    static bool registered = false;
    if (!registered) {
        WNDCLASSEXW wc{};
        wc.cbSize = sizeof(wc);
        wc.lpfnWndProc = PromptProc;
        wc.hInstance = inst;
        wc.hCursor = LoadCursorW(nullptr, MAKEINTRESOURCEW(32512));
        wc.lpszClassName = L"RavexPrompt";
        RegisterClassExW(&wc);
        registered = true;
    }
    PromptData data;
    if (!inst) inst = GetModuleHandleW(nullptr);
    RECT pr{}; GetWindowRect(parent, &pr);
    int pw = pr.right - pr.left; int ph = pr.bottom - pr.top;
    int dw = 384; int dh = 120;
    int x = pr.left + (pw - dw) / 2;
    int y = pr.top + (ph - dh) / 2;
    HMONITOR mon = MonitorFromWindow(parent, MONITOR_DEFAULTTONEAREST);
    MONITORINFO mi{}; mi.cbSize = sizeof(mi); GetMonitorInfoW(mon, &mi);
    RECT wr = mi.rcWork;
    if (x < wr.left) x = wr.left + 16;
    if (y < wr.top) y = wr.top + 16;
    if (x + dw > wr.right) x = wr.right - dw - 16;
    if (y + dh > wr.bottom) y = wr.bottom - dh - 16;
    HWND hwnd = CreateWindowExW(0, L"RavexPrompt", title.c_str(),
                                 WS_POPUP | WS_CAPTION | WS_SYSMENU,
                                 x, y, dw, dh, parent, nullptr, inst, &data);
    if (!hwnd) return false;
    ShowWindow(hwnd, SW_SHOW);
    UpdateWindow(hwnd);
    EnableWindow(parent, FALSE);
    MSG msg;
    while (!data.closed) {
        while (PeekMessageW(&msg, nullptr, 0, 0, PM_REMOVE)) {
            if (IsDialogMessageW(hwnd, &msg)) continue;
            TranslateMessage(&msg);
            DispatchMessageW(&msg);
        }
        Sleep(10);
    }
    EnableWindow(parent, TRUE);
    SetForegroundWindow(parent);
    if (data.ok) out = data.result;
    return data.ok;
}

int runMainWindow(HINSTANCE hInstance) {
    Gdiplus::GdiplusStartupInput si{}; ULONG_PTR gdiTok=0; Gdiplus::GdiplusStartup(&gdiTok, &si, nullptr);
    g_main.cfg = loadLauncherConfig();
    setCurrentLanguage(g_main.cfg.language.c_str());
    g_main.font = makeFont();
    g_main.iconFont = nullptr;
    g_main.theme = getThemeForConfig(g_main.cfg.theme, g_main.cfg.customBg, g_main.cfg.customPanel, g_main.cfg.customText, g_main.cfg.customAccent, g_main.cfg.customButton, g_main.cfg.customAlpha);
    g_main.bgBrush = CreateSolidBrush(g_main.theme.bg);
    g_main.panelBrush = CreateSolidBrush(g_main.theme.panel);

    WNDCLASSEXW wc{};
    wc.cbSize = sizeof(wc);
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = MainProc;
    wc.hInstance = hInstance;
    wc.hIcon = LoadIconW(hInstance, MAKEINTRESOURCEW(101));
    if (!wc.hIcon) wc.hIcon = LoadIconW(nullptr, MAKEINTRESOURCEW(32512));
    wc.hIconSm = LoadIconW(hInstance, MAKEINTRESOURCEW(101));
    wc.hCursor = LoadCursorW(nullptr, MAKEINTRESOURCEW(32512));
    wc.hbrBackground = g_main.bgBrush;
    wc.lpszClassName = L"RavexLauncherMain";
    if (!RegisterClassExW(&wc)) {
        DeleteObject(g_main.font);
        DeleteObject(g_main.bgBrush);
        DeleteObject(g_main.panelBrush);
        return 1;
    }

    int sw = 1100; int sh = 720;
    int sx = 0; int sy = 0;
    {
        HMONITOR mon = MonitorFromPoint({0,0}, MONITOR_DEFAULTTOPRIMARY);
        MONITORINFO mi{}; mi.cbSize = sizeof(mi); GetMonitorInfoW(mon, &mi);
        RECT wr = mi.rcWork;
        sx = wr.left + ((wr.right - wr.left) - sw) / 2;
        sy = wr.top + ((wr.bottom - wr.top) - sh) / 2;
        if (sx < wr.left) sx = wr.left + 16;
        if (sy < wr.top) sy = wr.top + 16;
    }
    std::wstring mainTitle = L"KickX Launcher v1.1 - " + fromUtf8(lang("for_windows"));
    HWND hwnd = CreateWindowExW(WS_EX_APPWINDOW, L"RavexLauncherMain", mainTitle.c_str(),
                                 WS_OVERLAPPEDWINDOW | WS_CLIPCHILDREN,
                                 sx, sy, sw, sh, nullptr, nullptr, hInstance, nullptr);
    if (!hwnd) {
        DeleteObject(g_main.font);
        DeleteObject(g_main.bgBrush);
        DeleteObject(g_main.panelBrush);
        return 1;
    }

    ShowWindow(hwnd, SW_SHOW);
    UpdateWindow(hwnd);
    RedrawWindow(hwnd, nullptr, nullptr, RDW_INVALIDATE | RDW_ALLCHILDREN | RDW_UPDATENOW | RDW_FRAME);

    MSG msg;
    while (GetMessageW(&msg, nullptr, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    if (g_main.font) DeleteObject(g_main.font);
    if (g_main.iconFont) DeleteObject(g_main.iconFont);
    DeleteObject(g_main.bgBrush);
    DeleteObject(g_main.panelBrush);
    Gdiplus::GdiplusShutdown(gdiTok);
    return static_cast<int>(msg.wParam);
}
}

