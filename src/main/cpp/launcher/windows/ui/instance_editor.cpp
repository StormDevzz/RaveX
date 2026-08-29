#include "ui/include/instance_editor.hpp"
#include "core/include/config.hpp"
#include "core/include/lang.hpp"
#include "core/include/paths.hpp"
#include "core/include/theme.hpp"
#include "core/include/util.hpp"
#include "ui/include/glow.hpp"
#include <cstring>
#include "game/include/fabric.hpp"
#include "net/include/http.hpp"
#include "net/include/json.hpp"
#include "resource.h"
#include <shellapi.h>
#include <commdlg.h>
#include <commctrl.h>
#include <uxtheme.h>
#include <windows.h>
#include <windowsx.h>
#include <gdiplus.h>
#include <cstdio>
#include <cstdlib>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include <atomic>
#include <wincodec.h>

namespace ravex::ui {

namespace {

constexpr int IDC_TAB = 2000;
constexpr int IDC_EDIT_NAME = 1101;
constexpr int IDC_COMBO_VER = 1102;
constexpr int IDC_EDIT_RAM = 1103;
constexpr int IDC_RAM_SPIN = 1104;
constexpr int IDC_EDIT_JVM = 1105;
constexpr int IDC_COMBO_LOADER = 1106;
constexpr int IDC_OPEN_FOLDER = 1107;
constexpr int IDC_EDIT_NOTES = 1108;
constexpr int IDC_NOTES_FRAME = 1109;
constexpr int IDC_NOTES_COUNTER = 1110;
constexpr int IDC_NOTES_HINT = 1111;
constexpr int IDC_EDIT_JAVA = 1112;
constexpr int IDC_BROWSE_JAVA = 1113;
constexpr int IDC_CHK_BUNDLED = 1114;
constexpr int IDC_CHK_OFFLINE = 1115;
constexpr int IDC_DUPLICATE = 1116;
constexpr int IDC_COMBO_LOADER_VER = 1117;
constexpr int IDC_MODS_PLATFORM = 2100;
constexpr int IDC_MODS_TYPE = 2101;
constexpr int IDC_MODS_SEARCH = 2102;
constexpr int IDC_MODS_GO = 2103;
constexpr int IDC_MODS_LIST = 2104;
constexpr int IDC_MODS_INSTALL = 2105;
constexpr int IDC_MODS_OPEN = 2106;
constexpr int IDC_MODS_SHOWINSTALLED = 2107;
constexpr int IDC_SERV_LIST = 2201;
constexpr int IDC_SERV_NAME = 2202;
constexpr int IDC_SERV_ADDR = 2203;
constexpr int IDC_SERV_ADD = 2204;
constexpr int IDC_SERV_DEL = 2205;
constexpr int IDC_SERV_OPEN = 2206;
constexpr int IDC_LOGS_LIST = 2301;
constexpr int IDC_LOGS_VIEW = 2302;
constexpr int IDC_LOGS_OPEN = 2303;
constexpr int IDC_LOGS_REFRESH = 2304;
constexpr UINT WM_APP_MODS = WM_APP + 50;

constexpr COLORREF kBg = RGB(28, 28, 30);
constexpr COLORREF kPanel = RGB(40, 40, 42);
constexpr COLORREF kText = RGB(225, 225, 225);

struct ModInfo {
    std::string slug;
    std::string title;
    std::string description;
    std::string iconUrl;
    HICON hIcon = nullptr;
};

HICON loadHIconEd(const std::wstring& path, int target);
std::wstring iconPathEd(const std::wstring& name);
void ensureGdiplus();
HICON loadViaWICForData(void* data, DWORD size, int target);
HICON loadHIconFromRes(int resId, int target);
HICON loadLauncherIcon(int resId, const std::wstring& name, int target) {
    HICON h = loadHIconFromRes(resId, target);
    if (h) return h;
    return loadHIconEd(iconPathEd(name), target);
}
HICON loadViaWICForData(void* data, DWORD size, int target);
HICON loadHIconFromRes(int resId, int target) {
    HRSRC hRes = FindResourceW(GetModuleHandleW(nullptr), MAKEINTRESOURCEW(resId), L"PNG");
    if (!hRes) hRes = FindResourceW(nullptr, MAKEINTRESOURCEW(resId), L"PNG");
    if (!hRes) return nullptr;
    HGLOBAL hData = LoadResource(GetModuleHandleW(nullptr), hRes);
    if (!hData) return nullptr;
    void* pData = LockResource(hData);
    DWORD size = SizeofResource(GetModuleHandleW(nullptr), hRes);
    if (!pData || !size) return nullptr;
    ensureGdiplus();
    IStream* stream = nullptr;
    if (FAILED(CreateStreamOnHGlobal(nullptr, TRUE, &stream))) return nullptr;
    ULONG written;
    stream->Write(pData, size, &written);
    LARGE_INTEGER li; li.QuadPart = 0; stream->Seek(li, STREAM_SEEK_SET, nullptr);
    Gdiplus::Bitmap* src = Gdiplus::Bitmap::FromStream(stream);
    HICON out = nullptr;
    if (src && src->GetLastStatus() == Gdiplus::Ok) {
        Gdiplus::Bitmap* dst = new Gdiplus::Bitmap(target, target, PixelFormat32bppARGB);
        Gdiplus::Graphics g(dst);
        g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
        g.DrawImage(src, Gdiplus::Rect(0,0,target,target), 0,0, src->GetWidth(), src->GetHeight(), Gdiplus::UnitPixel);
        dst->GetHICON(&out);
        delete dst;
    }
    if (src) delete src;
    stream->Release();
    if (out) return out;
    return loadViaWICForData(pData, size, target);
}
HICON loadViaWICForData(void* data, DWORD size, int target);
std::wstring iconCachePath(const std::string& url) {
    wchar_t tmp[MAX_PATH]; GetTempPathW(MAX_PATH, tmp);
    std::wstring dir = std::wstring(tmp) + L"ravex_icon_cache";
    CreateDirectoryW(dir.c_str(), nullptr);
    size_t h = std::hash<std::string>{}(url);
    wchar_t name[32]; swprintf(name, 32, L"%016llx.png", (unsigned long long)h);
    return dir + L"\\" + name;
}
static ULONG_PTR g_gdiToken = 0;
static bool g_gdiInit = false;
static std::mutex g_gdiMutex;
void ensureGdiplus() {
    std::lock_guard<std::mutex> lk(g_gdiMutex);
    if (!g_gdiInit) {
        Gdiplus::GdiplusStartupInput si{};
        Gdiplus::GdiplusStartup(&g_gdiToken, &si, nullptr);
        g_gdiInit = true;
    }
}
HICON loadViaWIC(const std::wstring& path, int target) {
    CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    IWICImagingFactory* factory = nullptr;
    if (FAILED(CoCreateInstance(CLSID_WICImagingFactory, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&factory)))) return nullptr;
    IWICBitmapDecoder* decoder = nullptr;
    HICON out = nullptr;
    if (SUCCEEDED(factory->CreateDecoderFromFilename(path.c_str(), nullptr, GENERIC_READ, WICDecodeMetadataCacheOnLoad, &decoder))) {
        IWICBitmapFrameDecode* frame = nullptr;
        if (SUCCEEDED(decoder->GetFrame(0, &frame))) {
            UINT w, h;
            frame->GetSize(&w, &h);
            IWICFormatConverter* conv = nullptr;
            if (SUCCEEDED(factory->CreateFormatConverter(&conv))) {
                if (SUCCEEDED(conv->Initialize(frame, GUID_WICPixelFormat32bppPBGRA, WICBitmapDitherTypeNone, nullptr, 0.0, WICBitmapPaletteTypeCustom))) {
                    ensureGdiplus();
                    Gdiplus::Bitmap* bmp = new Gdiplus::Bitmap(w, h, PixelFormat32bppARGB);
                    Gdiplus::BitmapData data;
                    Gdiplus::Rect rc(0, 0, w, h);
                    if (bmp->LockBits(&rc, Gdiplus::ImageLockModeWrite, PixelFormat32bppARGB, &data) == Gdiplus::Ok) {
                        conv->CopyPixels(nullptr, data.Stride, data.Stride * h, (BYTE*)data.Scan0);
                        bmp->UnlockBits(&data);
                        Gdiplus::Bitmap* dst = new Gdiplus::Bitmap(target, target, PixelFormat32bppARGB);
                        Gdiplus::Graphics g(dst);
                        g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
                        g.Clear(Gdiplus::Color(0,0,0,0));
                        g.DrawImage(bmp, Gdiplus::Rect(0,0,target,target), 0,0,w,h, Gdiplus::UnitPixel);
                        dst->GetHICON(&out);
                        delete dst;
                    }
                    delete bmp;
                }
                conv->Release();
            }
            frame->Release();
        }
        decoder->Release();
    }
    factory->Release();
    return out;
}
HICON loadViaWICForData(void* data, DWORD size, int target) {
    CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
    IStream* stream = nullptr;
    if (FAILED(CreateStreamOnHGlobal(nullptr, TRUE, &stream))) return nullptr;
    ULONG written; stream->Write(data, size, &written);
    LARGE_INTEGER li; li.QuadPart = 0; stream->Seek(li, STREAM_SEEK_SET, nullptr);
    IWICImagingFactory* factory = nullptr;
    HICON out = nullptr;
    if (SUCCEEDED(CoCreateInstance(CLSID_WICImagingFactory, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&factory)))) {
        IWICBitmapDecoder* decoder = nullptr;
        if (SUCCEEDED(factory->CreateDecoderFromStream(stream, nullptr, WICDecodeMetadataCacheOnLoad, &decoder))) {
            IWICBitmapFrameDecode* frame = nullptr;
            if (SUCCEEDED(decoder->GetFrame(0, &frame))) {
                UINT w, h; frame->GetSize(&w, &h);
                IWICFormatConverter* conv = nullptr;
                if (SUCCEEDED(factory->CreateFormatConverter(&conv))) {
                    if (SUCCEEDED(conv->Initialize(frame, GUID_WICPixelFormat32bppPBGRA, WICBitmapDitherTypeNone, nullptr, 0.0, WICBitmapPaletteTypeCustom))) {
                        ensureGdiplus();
                        Gdiplus::Bitmap* bmp = new Gdiplus::Bitmap(w, h, PixelFormat32bppARGB);
                        Gdiplus::BitmapData bd; Gdiplus::Rect rc(0,0,w,h);
                        if (bmp->LockBits(&rc, Gdiplus::ImageLockModeWrite, PixelFormat32bppARGB, &bd) == Gdiplus::Ok) {
                            conv->CopyPixels(nullptr, bd.Stride, bd.Stride * h, (BYTE*)bd.Scan0);
                            bmp->UnlockBits(&bd);
                            Gdiplus::Bitmap* dst = new Gdiplus::Bitmap(target, target, PixelFormat32bppARGB);
                            Gdiplus::Graphics g(dst);
                            g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
                            g.Clear(Gdiplus::Color(0,0,0,0));
                            g.DrawImage(bmp, Gdiplus::Rect(0,0,target,target), 0,0,w,h, Gdiplus::UnitPixel);
                            dst->GetHICON(&out);
                            delete dst;
                        }
                        delete bmp;
                    }
                    conv->Release();
                }
                frame->Release();
            }
            decoder->Release();
        }
        factory->Release();
    }
    stream->Release();
    return out;
}
HICON loadIconUrl(const std::string& url, int target) {
    try {
        if (url.empty()) return nullptr;
        ensureGdiplus();
        wchar_t tmp[MAX_PATH]; if (!GetTempPathW(MAX_PATH, tmp)) return nullptr;
        std::wstring path = std::wstring(tmp) + L"ravex_modicon.tmp";
        if (!net::downloadFile(url, path, {}, nullptr, nullptr)) return nullptr;
        HICON out = nullptr;
        Gdiplus::Bitmap* src = Gdiplus::Bitmap::FromFile(path.c_str());
        if (src && src->GetLastStatus() == Gdiplus::Ok) {
            Gdiplus::Bitmap* dst = new Gdiplus::Bitmap(target, target, PixelFormat32bppARGB);
            Gdiplus::Graphics g(dst);
            g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
            g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
            g.SetPixelOffsetMode(Gdiplus::PixelOffsetModeHighQuality);
            g.SetCompositingQuality(Gdiplus::CompositingQualityHighQuality);
            g.Clear(Gdiplus::Color(0,0,0,0));
            g.DrawImage(src, Gdiplus::Rect(0,0,target,target), 0,0, src->GetWidth(), src->GetHeight(), Gdiplus::UnitPixel);
            dst->GetHICON(&out);
            delete dst; delete src;
        } else {
            if (src) delete src;
            out = loadViaWIC(path, target);
        }
        DeleteFileW(path.c_str()); return out;
    } catch (...) { return nullptr; }
}
HICON loadIconUrlCached(const std::string& url, int target) {
    if (url.empty()) return nullptr;
    std::wstring cache = iconCachePath(url);
    if (fileExists(cache)) {
        ensureGdiplus();
        HICON out = nullptr;
        Gdiplus::Bitmap* src = Gdiplus::Bitmap::FromFile(cache.c_str());
        if (src && src->GetLastStatus() == Gdiplus::Ok) {
            Gdiplus::Bitmap* dst = new Gdiplus::Bitmap(target, target, PixelFormat32bppARGB);
            Gdiplus::Graphics g(dst);
            g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
            g.DrawImage(src, Gdiplus::Rect(0,0,target,target), 0,0, src->GetWidth(), src->GetHeight(), Gdiplus::UnitPixel);
            dst->GetHICON(&out);
            delete dst; delete src;
            if (out) return out;
        } else {
            if (src) delete src;
            out = loadViaWIC(cache, target);
            if (out) return out;
        }
    }
    if (!net::downloadFile(url, cache, {}, nullptr, nullptr)) {
        HICON fb = loadLauncherIcon(IDR_ICON_PACKAGE, L"package", target);
        return fb;
    }
    ensureGdiplus();
    HICON out = nullptr;
    Gdiplus::Bitmap* src = Gdiplus::Bitmap::FromFile(cache.c_str());
    if (src && src->GetLastStatus() == Gdiplus::Ok) {
        Gdiplus::Bitmap* dst = new Gdiplus::Bitmap(target, target, PixelFormat32bppARGB);
        Gdiplus::Graphics g(dst);
        g.SetInterpolationMode(Gdiplus::InterpolationModeHighQualityBicubic);
        g.DrawImage(src, Gdiplus::Rect(0,0,target,target), 0,0, src->GetWidth(), src->GetHeight(), Gdiplus::UnitPixel);
        dst->GetHICON(&out);
        delete dst; delete src;
        if (out) return out;
    } else {
        if (src) delete src;
        out = loadViaWIC(cache, target);
        if (out) return out;
    }
    if (!out) {
        DeleteFileW(cache.c_str());
        out = loadLauncherIcon(IDR_ICON_PACKAGE, L"package", target);
    }
    return out;
}
void parallelLoadIcons(std::vector<ModInfo>& res, int target) {
    if (res.empty()) return;
    std::atomic<size_t> idx{0};
    std::vector<std::thread> ths;
    int conc = 6;
    if ((int)res.size() < conc) conc = (int)res.size();
    for (int t = 0; t < conc; ++t) {
        ths.emplace_back([&]() {
            while (true) {
                size_t i = idx.fetch_add(1);
                if (i >= res.size()) break;
                try { res[i].hIcon = loadIconUrlCached(res[i].iconUrl, target); } catch (...) { res[i].hIcon = nullptr; }
            }
        });
    }
    for (auto& th : ths) th.join();
}

struct Server {
    std::string name;
    std::string address;
};

std::wstring iconPathEd(const std::wstring& name) {
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
HICON loadHIconEd(const std::wstring& path, int target) {
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
    HICON out = nullptr;
    dst->GetHICON(&out);
    delete dst;
    return out;
}
void setBtnIconEd(HWND btn, const std::wstring& name) {
    std::wstring p = iconPathEd(name);
    if (!fileExists(p)) return;
    bool iconOnly = GetWindowTextLengthW(btn) == 0;
    int sz = iconOnly ? 20 : 18;
    HICON hIc = loadHIconEd(p, sz);
    if (!hIc) return;
    if (iconOnly) {
        LONG_PTR st = GetWindowLongPtrW(btn, GWL_STYLE);
        st = (st & ~BS_TYPEMASK) | BS_ICON;
        SetWindowLongPtrW(btn, GWL_STYLE, st);
        SendMessageW(btn, BM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIc));
    } else {
        HIMAGELIST hil = ImageList_Create(sz, sz, ILC_COLOR32 | ILC_MASK, 1, 1);
        ImageList_AddIcon(hil, hIc);
        DestroyIcon(hIc);
        BUTTON_IMAGELIST bi{}; bi.himl = hil; bi.uAlign = BUTTON_IMAGELIST_ALIGN_LEFT; bi.margin.left = 6; bi.margin.right = 4;
        SendMessageW(btn, BCM_SETIMAGELIST, 0, reinterpret_cast<LPARAM>(&bi));
    }
}

struct EditorData {
    bool closed = false;
    bool ok = false;
    InstanceCfg cfg;
    ThemeColors theme;
    HBRUSH bgBrush = nullptr;
    HBRUSH panelBrush = nullptr;
    std::vector<HWND> tabBtns;
    int curTab = 0;
    int animIndicatorX = 0;
    std::vector<HICON> tabIcons;
    HFONT font = nullptr;
    HFONT titleFont = nullptr;
    HFONT smallFont = nullptr;
    std::vector<std::string> versions;
    GlowData glow;
    int scrollPos = 0;
    int maxScroll = 0;
    int bounce = 0;
    HWND editName = nullptr;
    HWND iconName = nullptr;
    HWND comboVer = nullptr;
    HWND iconVer = nullptr;
    HWND comboLoader = nullptr;
    HWND iconLoaderLbl = nullptr;
    HWND comboLoaderVer = nullptr;
    HWND lblLoaderVer = nullptr;
    HWND iconLoaderVer = nullptr;
    HWND editRam = nullptr;
    HWND iconRam = nullptr;
    HWND spinRam = nullptr;
    HWND editJvm = nullptr;
    HWND iconJvm = nullptr;
    HWND iconJava = nullptr;
    HWND editNotes = nullptr;
    HWND notesFrame = nullptr;
    HWND notesTitleIcon = nullptr;
    HWND notesTitle = nullptr;
    HWND notesHint = nullptr;
    HWND notesCounter = nullptr;
    HWND editJava = nullptr;
    HWND btnBrowse = nullptr;
    HWND chkBundle = nullptr;
    std::vector<HICON> fieldIcons;
    HWND btnFolder = nullptr;
    HWND btnDuplicate = nullptr;
    HWND btnOk = nullptr;
    HWND btnCancel = nullptr;
    int prevTab = -1;
    float tabFade = 0.0f;
    HWND comboModsPlatform = nullptr;
    HWND comboModsType = nullptr;
    HWND editModsSearch = nullptr;
    HWND btnModsGo = nullptr;
    HWND listMods = nullptr;
    HWND btnModsInstall = nullptr;
    HWND btnModsOpen = nullptr;
    HWND btnModsShowInstalled = nullptr;
    HWND hModDetails = nullptr;
    HWND listServers = nullptr;
    HWND editServName = nullptr;
    HWND editServAddr = nullptr;
    HWND btnServAdd = nullptr;
    HWND btnServDel = nullptr;
    HWND btnServOpen = nullptr;
    HWND listLogs = nullptr;
    HWND editLogView = nullptr;
    HWND btnLogsOpen = nullptr;
    HWND btnLogsRefresh = nullptr;
    std::vector<HWND> generalCtrls;
    std::vector<HWND> modsCtrls;
    std::vector<HWND> serversCtrls;
    std::vector<HWND> notesCtrls;
    std::vector<HWND> logsCtrls;
    std::vector<ModInfo> modResults;
    std::vector<Server> servers;
    std::vector<std::wstring> logFiles;
};

HFONT makeFont() {
    HFONT f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       VARIABLE_PITCH | FF_SWISS, L"Manrope");
    if (!f) f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    if (!f) f = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    return f;
}

std::string urlEncode(const std::string& s) {
    std::string out;
    for (char c : s) {
        unsigned char uc = static_cast<unsigned char>(c);
        if (isalnum(uc) || c == '-' || c == '_' || c == '.' || c == '~') out += c;
        else { char buf[5]; std::snprintf(buf, sizeof(buf), "%%%02X", uc); out += buf; }
    }
    return out;
}

std::string jsonEscape(const std::string& s) {
    std::string out = "\"";
    for (char c : s) {
        if (c == '"' || c == '\\') { out += '\\'; out += c; }
        else out += c;
    }
    out += "\"";
    return out;
}

WNDPROC wheelOldProc(HWND w) {
    return reinterpret_cast<WNDPROC>(GetPropW(w, L"wheelOldProc"));
}
LRESULT CALLBACK wheelRedirectProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    if (msg == WM_MOUSEWHEEL) {
        HWND parent = GetParent(hwnd);
        if (parent) { PostMessageW(parent, WM_MOUSEWHEEL, wParam, lParam); return 0; }
    }
    WNDPROC old = wheelOldProc(hwnd);
    if (old) return CallWindowProcW(old, hwnd, msg, wParam, lParam);
    return DefWindowProcW(hwnd, msg, wParam, lParam);
}
void subclassWheel(HWND child) {
    if (!child) return;
    WNDPROC old = reinterpret_cast<WNDPROC>(GetWindowLongPtrW(child, GWLP_WNDPROC));
    SetPropW(child, L"wheelOldProc", reinterpret_cast<HANDLE>(old));
    SetWindowLongPtrW(child, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(wheelRedirectProc));
}

void updateModsInstallButton(EditorData* data) {
    if (!data || !data->btnModsInstall || !data->comboLoader || !data->comboModsType) return;
    int ls = static_cast<int>(SendMessageW(data->comboLoader, CB_GETCURSEL, 0, 0));
    std::string loader = "vanilla"; if (ls == 1) loader = "fabric"; else if (ls == 2) loader = "forge"; else if (ls == 3) loader = "quilt";
    int typeSel = static_cast<int>(SendMessageW(data->comboModsType, CB_GETCURSEL, 0, 0));
    bool isModOrShader = (typeSel == 0 || typeSel == 2);
    bool block = (loader == "vanilla" && isModOrShader);
    EnableWindow(data->btnModsInstall, block ? FALSE : TRUE);
}
void reloadLoaderVersions(EditorData* data) {
    if (!data || !data->comboLoader || !data->comboLoaderVer) return;
    try {
        int ls = static_cast<int>(SendMessageW(data->comboLoader, CB_GETCURSEL, 0, 0));
        std::string loader = "vanilla";
        if (ls == 1) loader = "fabric";
        else if (ls == 2) loader = "forge";
        else if (ls == 3) loader = "quilt";
        std::wstring mv;
        int l = GetWindowTextLengthW(data->comboVer);
        if (l > 0) { mv.resize(static_cast<std::size_t>(l+1)); GetWindowTextW(data->comboVer, mv.data(), l + 1); if (l>0) mv.resize(l); else mv.clear(); }
        SendMessageW(data->comboLoaderVer, CB_RESETCONTENT, 0, 0);
        if (loader == "vanilla") {
            if (data->lblLoaderVer) ShowWindow(data->lblLoaderVer, SW_HIDE);
            if (data->iconLoaderVer) ShowWindow(data->iconLoaderVer, SW_HIDE);
            ShowWindow(data->comboLoaderVer, SW_HIDE);
        } else {
            if (data->lblLoaderVer) ShowWindow(data->lblLoaderVer, SW_SHOW);
            if (data->iconLoaderVer) ShowWindow(data->iconLoaderVer, SW_SHOW);
            ShowWindow(data->comboLoaderVer, SW_SHOW);
            std::vector<std::string> vers;
            if (loader == "fabric") vers = game::fetchFabricLoaderVersions(toUtf8(mv));
            else if (loader == "forge") vers = game::fetchForgeLoaderVersions(toUtf8(mv));
            else if (loader == "quilt") vers = game::fetchQuiltLoaderVersions(toUtf8(mv));
            for (const std::string& v : vers) SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(v).c_str()));
            if (vers.empty()) { const char* _lk=ravex::lang("latest"); SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>((_lk&&strcmp(_lk,"latest")!=0?fromUtf8(_lk):fromUtf8("Latest")).c_str())); }
            SendMessageW(data->comboLoaderVer, CB_SETCURSEL, 0, 0);
        }
        updateModsInstallButton(data);
    } catch (...) {}
}

std::vector<ModInfo> modrinthSearch(const std::string&, const std::string&, const std::string&);
std::vector<ModInfo> curseforgeSearch(const std::string& query, const std::string& mc, const std::string& type) {
    std::vector<ModInfo> out;
    std::string cfType = "mc-mods";
    if (type == "resourcepack") cfType = "texture-packs";
    else if (type == "shader") cfType = "shaders";
    std::string url = "https://api.curseforge.com/v1/mods/search?gameId=432&searchFilter=" + urlEncode(query) + "&classId=6&sortField=2&sortOrder=desc&pageSize=25";
    if (!mc.empty()) url += "&gameVersion=" + urlEncode(mc);
    std::string body = net::httpGet(url, nullptr);
    if (body.empty()) {
        ModInfo m; m.slug = ""; m.title = "CurseForge requires API key - showing Modrinth";
        return modrinthSearch(query, mc, type);
    }
    ravex::json::Value root = ravex::json::Value::parse(body);
    if (root.isNull() || !root.has("data")) return out;
    const ravex::json::Value& arr = root.at("data");
    for (size_t i = 0; i < arr.size(); ++i) {
        const ravex::json::Value& h = arr.at(i);
        ModInfo m;
        if (h.has("slug")) m.slug = h.at("slug").asString();
        else if (h.has("name")) m.slug = h.at("name").asString();
        if (h.has("name")) m.title = h.at("name").asString();
        if (h.has("summary")) m.description = h.at("summary").asString();
        if (h.has("logo") && h.at("logo").has("url")) m.iconUrl = h.at("logo").at("url").asString();
        out.push_back(m);
    }
    return out;
}
std::vector<ModInfo> modrinthSearch(const std::string& query, const std::string& mc, const std::string& type) {
    std::vector<ModInfo> out;
    std::string facets = "[[\"project_type:" + type + "\"]";
    if (!mc.empty()) facets += ",[\"versions:" + mc + "\"]";
    facets += "]";
    std::string idx = query.empty() ? "downloads" : "relevance";
    std::string url = "https://api.modrinth.com/v2/search?query=" + urlEncode(query) +
                      "&limit=25&index=" + idx + "&facets=" + urlEncode(facets);
    std::string body = net::httpGet(url, nullptr);
    if (body.empty()) return out;
    ravex::json::Value root = ravex::json::Value::parse(body);
    if (root.isNull() || !root.has("hits")) return out;
    const ravex::json::Value& hits = root.at("hits");
    for (size_t i = 0; i < hits.size(); ++i) {
        const ravex::json::Value& h = hits.at(i);
        ModInfo m;
        if (h.has("slug")) m.slug = h.at("slug").asString();
        if (h.has("title")) m.title = h.at("title").asString();
        if (h.has("description")) m.description = h.at("description").asString();
        if (h.has("icon_url")) m.iconUrl = h.at("icon_url").asString();
        out.push_back(m);
    }
    return out;
}

bool modrinthInstall(const std::string& slug, const std::string& mc, const std::wstring& destDir, std::string* error) {
    std::string url = "https://api.modrinth.com/v2/project/" + slug + "/version";
    std::string body = net::httpGet(url, error);
    if (body.empty()) return false;
    ravex::json::Value vers = ravex::json::Value::parse(body);
    if (vers.isNull()) return false;
    for (size_t i = 0; i < vers.size(); ++i) {
        const ravex::json::Value& v = vers.at(i);
        bool match = mc.empty();
        if (!match && v.has("game_versions")) {
            const ravex::json::Value& gv = v.at("game_versions");
            for (size_t j = 0; j < gv.size(); ++j) {
                if (gv.at(j).asString() == mc) { match = true; break; }
            }
        }
        if (match && v.has("files")) {
            const ravex::json::Value& files = v.at("files");
            for (size_t k = 0; k < files.size(); ++k) {
                const ravex::json::Value& f = files.at(k);
                std::string furl = f.has("url") ? f.at("url").asString() : "";
                std::string fname = f.has("filename") ? f.at("filename").asString() : "";
                if (!furl.empty() && !fname.empty()) {
                    std::wstring dest = joinPath(destDir, fromUtf8(fname));
                    if (net::downloadFile(furl, dest, {}, nullptr, error)) return true;
                }
            }
        }
    }
    *error = "No compatible file found for " + mc;
    return false;
}

std::vector<Server> loadServers(const std::wstring& dir) {
    std::vector<Server> out;
    std::string text;
    if (!readFile(joinPath(dir, L"servers.json"), text)) return out;
    ravex::json::Value root = ravex::json::Value::parse(text);
    if (root.isNull() || root.type() != ravex::json::Value::Type::Array) return out;
    for (size_t i = 0; i < root.size(); ++i) {
        const ravex::json::Value& o = root.at(i);
        Server s;
        if (o.has("name")) s.name = o.at("name").asString();
        if (o.has("address")) s.address = o.at("address").asString();
        out.push_back(s);
    }
    return out;
}

void saveServers(const std::wstring& dir, const std::vector<Server>& list) {
    createDirs(dir);
    std::string out = "[\n";
    for (size_t i = 0; i < list.size(); ++i) {
        if (i > 0) out += ",\n";
        out += "  {\"name\": " + jsonEscape(list[i].name) + ", \"address\": " + jsonEscape(list[i].address) + "}";
    }
    out += "\n]\n";
    writeFileAtomic(joinPath(dir, L"servers.json"), out);
}

std::vector<std::wstring> loadLogFiles(const std::wstring& dir) {
    std::vector<std::wstring> out;
    std::wstring logs = joinPath(dir, L"logs");
    if (!fileExists(logs)) return out;
    std::wstring pat = joinPath(logs, L"*");
    WIN32_FIND_DATAW fd;
    HANDLE h = FindFirstFileW(pat.c_str(), &fd);
    if (h == INVALID_HANDLE_VALUE) return out;
    do {
        if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
            std::wstring n = fd.cFileName;
            if (n.size() >= 4 && (n.compare(n.size() - 4, 4, L".log") == 0 || n.compare(n.size() - 4, 4, L".txt") == 0))
                out.push_back(joinPath(logs, n));
        }
    } while (FindNextFileW(h, &fd));
    FindClose(h);
    return out;
}

void applyTab(EditorData* d, int idx) {
    auto show = [idx](std::vector<HWND>& grp, int tab) {
        for (HWND w : grp) if (w) ShowWindow(w, idx == tab ? SW_SHOW : SW_HIDE);
    };
    show(d->generalCtrls, 0);
    if (idx == 0 && d && d->comboLoader && d->comboLoaderVer) {
        int ls = static_cast<int>(SendMessageW(d->comboLoader, CB_GETCURSEL, 0, 0));
        std::string loader = "vanilla";
        if (ls == 1) loader = "fabric";
        else if (ls == 2) loader = "forge";
        else if (ls == 3) loader = "quilt";
        if (loader == "vanilla") {
            if (d->lblLoaderVer) ShowWindow(d->lblLoaderVer, SW_HIDE);
            if (d->iconLoaderVer) ShowWindow(d->iconLoaderVer, SW_HIDE);
            ShowWindow(d->comboLoaderVer, SW_HIDE);
        }
    }
    show(d->modsCtrls, 1);
    show(d->serversCtrls, 2);
    show(d->notesCtrls, 3);
    show(d->logsCtrls, 4);
}

LRESULT CALLBACK EditorProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    EditorData* data = reinterpret_cast<EditorData*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
    switch (msg) {
        case WM_CREATE: {
            CREATESTRUCTW* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            data = reinterpret_cast<EditorData*>(cs->lpCreateParams);
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(data));
            INITCOMMONCONTROLSEX icc{};
            icc.dwSize = sizeof(icc);
            icc.dwICC = ICC_WIN95_CLASSES | ICC_COOL_CLASSES | ICC_USEREX_CLASSES | ICC_PROGRESS_CLASS | ICC_TAB_CLASSES;
            InitCommonControlsEx(&icc);
            LauncherConfig lcfg = loadLauncherConfig();
            setCurrentLanguage(lcfg.language.c_str());
            data->theme = getThemeForConfig(lcfg.theme, lcfg.customBg, lcfg.customPanel, lcfg.customText, lcfg.customAccent);
            data->bgBrush = CreateSolidBrush(data->theme.bg);
            data->panelBrush = CreateSolidBrush(data->theme.panel);
            applyWindowTheme(hwnd, data->theme);
            data->font = makeFont();
            data->smallFont = CreateFontW(-11, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
            if (!data->smallFont) data->smallFont = CreateFontW(-11, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET, OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY, VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
            data->titleFont = makeFont();
            HINSTANCE inst = cs->hInstance;

            int dpi = 96;
            #if defined(_WIN32_WINNT) && _WIN32_WINNT >= 0x0A00
            if (HMODULE u = GetModuleHandleW(L"user32.dll")) {
                auto fn = reinterpret_cast<UINT(WINAPI*)(HWND)>(GetProcAddress(u, "GetDpiForWindow"));
                if (fn) dpi = (int)fn(hwnd);
            }
            #endif
            if (dpi < 96) dpi = 96;
            auto sc = [&](int v){ return MulDiv(v, dpi, 96); };
            {
                auto tr = [&](const char* k, const char* fb){ const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v); };
                std::wstring wnames[5] = {tr("general","General"), tr("mods","Mods"), tr("servers","Servers"), tr("notes","Notes"), tr("logs","Logs")};
                const wchar_t* names[] = {wnames[0].c_str(), wnames[1].c_str(), wnames[2].c_str(), wnames[3].c_str(), wnames[4].c_str()};
                const wchar_t* iconNames[] = {L"settings", L"mods", L"servers", L"notes", L"logs"};
                int s = sc(16);
                int totalW = sc(948);
                int gap = sc(6);
                int btnH = sc(36);
                int btnW = (totalW - gap * 4) / 5;
                for (int i = 0; i < 5; ++i) {
                    int x = 16 + i * (btnW + gap);
                    if (i == 4) btnW = totalW - (x - 16) - 0;
                    HWND b = CreateWindowExW(0, L"BUTTON", names[i], WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS | BS_OWNERDRAW, x, 56, btnW, btnH, hwnd, reinterpret_cast<HMENU>(static_cast<INT_PTR>(IDC_TAB + i)), inst, nullptr);
                    SendMessageW(b, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
                    data->tabBtns.push_back(b);
                    std::wstring p = iconPathEd(iconNames[i]);
                    if (!fileExists(p) && i == 1) p = iconPathEd(L"mods");
                    if (!fileExists(p) && i == 3) p = iconPathEd(L"edit");
                    if (!fileExists(p) && i == 4) p = iconPathEd(L"update");
                    HICON hIc = fileExists(p) ? loadHIconEd(p, s) : nullptr;
                    data->tabIcons.push_back(hIc);
                }
                data->curTab = 0;
            }

            auto tr2=[&](const char* k,const char* fb){const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
            auto trI=[&](const char* k,const char* fb){const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
            HWND header = CreateWindowExW(0, L"STATIC", trI("instance_settings","Instance Settings").c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT, 24, 92, 820, 30, hwnd, nullptr, inst, nullptr);
            SendMessageW(header, WM_SETFONT, reinterpret_cast<WPARAM>(data->titleFont), TRUE);
            data->generalCtrls.push_back(header);

            HWND iconName = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 24, 132, 16, 16, hwnd, nullptr, inst, nullptr);
            HICON hIconName = loadLauncherIcon(IDR_ICON_FIELD_NAME, L"field_name", 16);
            if (hIconName) { SendMessageW(iconName, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIconName)); data->fieldIcons.push_back(hIconName); }
            data->iconName = iconName;
            HWND lblName = CreateWindowExW(0, L"STATIC", trI("name","Name").c_str(), WS_CHILD | WS_VISIBLE, 44, 130, 800, 22, hwnd, nullptr, inst, nullptr);
            data->editName = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 24, 154, 820, 34, hwnd, reinterpret_cast<HMENU>(IDC_EDIT_NAME), inst, nullptr);
            SetWindowTextW(data->editName, fromUtf8(data->cfg.name).c_str());
            data->generalCtrls.push_back(iconName); data->generalCtrls.push_back(lblName); data->generalCtrls.push_back(data->editName);

            HWND iconVer = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 24, 206, 16, 16, hwnd, nullptr, inst, nullptr);
            HICON hIconVer = loadLauncherIcon(IDR_ICON_FIELD_VERSION, L"field_version", 16);
            if (hIconVer) { SendMessageW(iconVer, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIconVer)); data->fieldIcons.push_back(hIconVer); }
            data->iconVer = iconVer;
            HWND iconLoaderLbl = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 460, 206, 16, 16, hwnd, nullptr, inst, nullptr);
            HICON hIconLoaderLbl = loadLauncherIcon(IDR_ICON_FIELD_LOADER, L"field_loader", 16);
            if (hIconLoaderLbl) { SendMessageW(iconLoaderLbl, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIconLoaderLbl)); data->fieldIcons.push_back(hIconLoaderLbl); }
            data->iconLoaderLbl = iconLoaderLbl;
            HWND lblVer = CreateWindowExW(0, L"STATIC", trI("minecraft_version","Minecraft Version").c_str(), WS_CHILD | WS_VISIBLE, 44, 204, 380, 22, hwnd, nullptr, inst, nullptr);
            HWND lblLoader = CreateWindowExW(0, L"STATIC", trI("loader","Loader").c_str(), WS_CHILD | WS_VISIBLE, 480, 204, 380, 22, hwnd, nullptr, inst, nullptr);
            data->comboVer = CreateWindowExW(0, L"COMBOBOX", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | CBS_DROPDOWN | WS_VSCROLL, 24, 228, 400, 240, hwnd, reinterpret_cast<HMENU>(IDC_COMBO_VER), inst, nullptr);
            for (const std::string& v : data->versions) SendMessageW(data->comboVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(v).c_str()));
            if (!data->cfg.mcVersion.empty()) {
                std::wstring cur = fromUtf8(data->cfg.mcVersion);
                LRESULT idx = SendMessageW(data->comboVer, CB_FINDSTRINGEXACT, static_cast<WPARAM>(-1), reinterpret_cast<LPARAM>(cur.c_str()));
                if (idx != CB_ERR) SendMessageW(data->comboVer, CB_SETCURSEL, idx, 0);
                else SetWindowTextW(data->comboVer, cur.c_str());
            } else if (!data->versions.empty()) SendMessageW(data->comboVer, CB_SETCURSEL, 0, 0);
            if (data->versions.empty()) SetWindowTextW(data->comboVer, fromUtf8(data->cfg.mcVersion).c_str());
            data->comboLoader = CreateWindowExW(0, L"COMBOBOX", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | CBS_DROPDOWNLIST | WS_VSCROLL, 460, 228, 400, 240, hwnd, reinterpret_cast<HMENU>(IDC_COMBO_LOADER), inst, nullptr);
            SendMessageW(data->comboLoader, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("vanilla","Vanilla").c_str()));
            SendMessageW(data->comboLoader, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("fabric","Fabric").c_str()));
            SendMessageW(data->comboLoader, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("forge","Forge").c_str()));
            SendMessageW(data->comboLoader, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("quilt","Quilt").c_str()));
            {
                int sel = 0;
                if (data->cfg.loader == "fabric") sel = 1;
                else if (data->cfg.loader == "forge") sel = 2;
                else if (data->cfg.loader == "quilt") sel = 3;
                SendMessageW(data->comboLoader, CB_SETCURSEL, sel, 0);
            }
            data->generalCtrls.push_back(iconVer); data->generalCtrls.push_back(iconLoaderLbl);
            data->generalCtrls.push_back(lblVer); data->generalCtrls.push_back(lblLoader);
            data->generalCtrls.push_back(data->comboVer); data->generalCtrls.push_back(data->comboLoader);

            HWND iconLoaderVer = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 460, 342, 16, 16, hwnd, nullptr, inst, nullptr);
            HICON hIconLoaderVer = loadLauncherIcon(IDR_ICON_FIELD_LOADERVER, L"field_loaderver", 16);
            if (hIconLoaderVer) { SendMessageW(iconLoaderVer, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIconLoaderVer)); data->fieldIcons.push_back(hIconLoaderVer); }
            data->iconLoaderVer = iconLoaderVer;
            data->lblLoaderVer = CreateWindowExW(0, L"STATIC", trI("loader_version","Loader Version").c_str(), WS_CHILD | WS_VISIBLE, 480, 340, 380, 22, hwnd, nullptr, inst, nullptr);
            HWND lblLoaderVer = data->lblLoaderVer;
            data->comboLoaderVer = CreateWindowExW(0, L"COMBOBOX", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | CBS_DROPDOWNLIST | WS_VSCROLL, 480, 364, 380, 200, hwnd, reinterpret_cast<HMENU>(IDC_COMBO_LOADER_VER), inst, nullptr);
            if (data->cfg.loader == "fabric") {
                std::vector<std::string> vers = game::fetchFabricLoaderVersions(data->cfg.mcVersion);
                for (const std::string& v : vers) SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(v).c_str()));
                if (vers.empty()) { SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("latest","Latest").c_str())); SendMessageW(data->comboLoaderVer, CB_SETCURSEL, 0, 0); }
                else if (!data->cfg.loaderVersion.empty()) {
                    LRESULT idx = SendMessageW(data->comboLoaderVer, CB_FINDSTRINGEXACT, static_cast<WPARAM>(-1), reinterpret_cast<LPARAM>(fromUtf8(data->cfg.loaderVersion).c_str()));
                    if (idx != CB_ERR) SendMessageW(data->comboLoaderVer, CB_SETCURSEL, idx, 0); else SetWindowTextW(data->comboLoaderVer, fromUtf8(data->cfg.loaderVersion).c_str());
                } else SendMessageW(data->comboLoaderVer, CB_SETCURSEL, 0, 0);
            } else if (data->cfg.loader == "forge") {
                std::vector<std::string> vers = game::fetchForgeLoaderVersions(data->cfg.mcVersion);
                for (const std::string& v : vers) SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(v).c_str()));
                if (vers.empty()) { SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("latest","Latest").c_str())); SendMessageW(data->comboLoaderVer, CB_SETCURSEL, 0, 0); }
                else if (!data->cfg.loaderVersion.empty()) {
                    LRESULT idx = SendMessageW(data->comboLoaderVer, CB_FINDSTRINGEXACT, static_cast<WPARAM>(-1), reinterpret_cast<LPARAM>(fromUtf8(data->cfg.loaderVersion).c_str()));
                    if (idx != CB_ERR) SendMessageW(data->comboLoaderVer, CB_SETCURSEL, idx, 0); else SetWindowTextW(data->comboLoaderVer, fromUtf8(data->cfg.loaderVersion).c_str());
                } else SendMessageW(data->comboLoaderVer, CB_SETCURSEL, 0, 0);
            } else if (data->cfg.loader == "quilt") {
                std::vector<std::string> vers = game::fetchQuiltLoaderVersions(data->cfg.mcVersion);
                for (const std::string& v : vers) SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(fromUtf8(v).c_str()));
                if (vers.empty()) { SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("latest","Latest").c_str())); SendMessageW(data->comboLoaderVer, CB_SETCURSEL, 0, 0); }
                else if (!data->cfg.loaderVersion.empty()) {
                    LRESULT idx = SendMessageW(data->comboLoaderVer, CB_FINDSTRINGEXACT, static_cast<WPARAM>(-1), reinterpret_cast<LPARAM>(fromUtf8(data->cfg.loaderVersion).c_str()));
                    if (idx != CB_ERR) SendMessageW(data->comboLoaderVer, CB_SETCURSEL, idx, 0); else SetWindowTextW(data->comboLoaderVer, fromUtf8(data->cfg.loaderVersion).c_str());
                } else SendMessageW(data->comboLoaderVer, CB_SETCURSEL, 0, 0);
            } else { SendMessageW(data->comboLoaderVer, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("latest","Latest").c_str())); SendMessageW(data->comboLoaderVer, CB_SETCURSEL, 0, 0); }
            if (data->cfg.loader == "vanilla") { ShowWindow(lblLoaderVer, SW_HIDE); ShowWindow(data->comboLoaderVer, SW_HIDE); ShowWindow(iconLoaderVer, SW_HIDE); }
            data->generalCtrls.push_back(iconLoaderVer); data->generalCtrls.push_back(lblLoaderVer); data->generalCtrls.push_back(data->comboLoaderVer);

            HWND iconRam = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 24, 342, 16, 16, hwnd, nullptr, inst, nullptr);
            HICON hIconRam = loadLauncherIcon(IDR_ICON_FIELD_RAM, L"field_ram", 16);
            if (hIconRam) { SendMessageW(iconRam, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIconRam)); data->fieldIcons.push_back(hIconRam); }
            data->iconRam = iconRam;
            HWND lblRam = CreateWindowExW(0, L"STATIC", trI("ram_mb","RAM (MB)").c_str(), WS_CHILD | WS_VISIBLE, 44, 340, 380, 22, hwnd, nullptr, inst, nullptr);
            data->editRam = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL | ES_NUMBER, 24, 364, 200, 34, hwnd, reinterpret_cast<HMENU>(IDC_EDIT_RAM), inst, nullptr);
            SetWindowTextW(data->editRam, std::to_wstring(data->cfg.ramMb).c_str());
            data->spinRam = CreateWindowExW(0, UPDOWN_CLASSW, nullptr, WS_CHILD | WS_VISIBLE | UDS_SETBUDDYINT | UDS_ALIGNRIGHT | UDS_ARROWKEYS, 0, 0, 0, 0, hwnd, reinterpret_cast<HMENU>(IDC_RAM_SPIN), inst, nullptr);
            SendMessageW(data->spinRam, UDM_SETBUDDY, reinterpret_cast<WPARAM>(data->editRam), 0);
            SendMessageW(data->spinRam, UDM_SETRANGE32, 512, 32768);
            data->generalCtrls.push_back(iconRam); data->generalCtrls.push_back(lblRam); data->generalCtrls.push_back(data->editRam); data->generalCtrls.push_back(data->spinRam);

            HWND iconJvm = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 24, 416, 16, 16, hwnd, nullptr, inst, nullptr);
            HICON hIconJvm = loadLauncherIcon(IDR_ICON_FIELD_JVM, L"field_jvm", 16);
            if (hIconJvm) { SendMessageW(iconJvm, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIconJvm)); data->fieldIcons.push_back(hIconJvm); }
            data->iconJvm = iconJvm;
            HWND lblJvm = CreateWindowExW(0, L"STATIC", trI("jvm_args","JVM Arguments").c_str(), WS_CHILD | WS_VISIBLE, 44, 414, 800, 22, hwnd, nullptr, inst, nullptr);
            data->editJvm = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 24, 438, 820, 34, hwnd, reinterpret_cast<HMENU>(IDC_EDIT_JVM), inst, nullptr);
            SetWindowTextW(data->editJvm, fromUtf8(data->cfg.jvmArgs).c_str());
            data->generalCtrls.push_back(iconJvm); data->generalCtrls.push_back(lblJvm); data->generalCtrls.push_back(data->editJvm);

            HWND iconJava = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 24, 490, 16, 16, hwnd, nullptr, inst, nullptr);
            HICON hIconJava = loadLauncherIcon(IDR_ICON_FIELD_JAVA, L"field_java", 16);
            if (hIconJava) { SendMessageW(iconJava, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hIconJava)); data->fieldIcons.push_back(hIconJava); }
            data->iconJava = iconJava;
            HWND lblJava = CreateWindowExW(0, L"STATIC", (trI("java_path","Java Path")+L" ("+trI("java_path_hint","leave empty to use bundled Java") + L")").c_str(), WS_CHILD | WS_VISIBLE, 44, 488, 800, 22, hwnd, nullptr, inst, nullptr);
            data->editJava = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 24, 512, 680, 34, hwnd, reinterpret_cast<HMENU>(IDC_EDIT_JAVA), inst, nullptr);
            SetWindowTextW(data->editJava, fromUtf8(data->cfg.javaPath).c_str());
            data->btnBrowse = CreateWindowExW(0, L"BUTTON", trI("browse","Browse...").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 716, 512, 140, 34, hwnd, reinterpret_cast<HMENU>(IDC_BROWSE_JAVA), inst, nullptr);
            data->generalCtrls.push_back(iconJava); data->generalCtrls.push_back(lblJava); data->generalCtrls.push_back(data->editJava); data->generalCtrls.push_back(data->btnBrowse);

            data->chkBundle = CreateWindowExW(0, L"BUTTON", trI("use_bundled_java","Use bundled Java").c_str(), WS_CHILD | WS_VISIBLE | BS_OWNERDRAW | WS_TABSTOP, 24, 560, 820, 24, hwnd, reinterpret_cast<HMENU>(IDC_CHK_BUNDLED), inst, nullptr);
            SendMessageW(data->chkBundle, BM_SETCHECK, data->cfg.useBundledJava ? BST_CHECKED : BST_UNCHECKED, 0);
            data->generalCtrls.push_back(data->chkBundle);

            data->btnFolder = CreateWindowExW(0, L"BUTTON", trI("open_folder","Open Folder").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW, 24, 620, 150, 36, hwnd, reinterpret_cast<HMENU>(IDC_OPEN_FOLDER), inst, nullptr);
            data->btnDuplicate = CreateWindowExW(0, L"BUTTON", trI("duplicate","Duplicate").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW, 192, 620, 150, 36, hwnd, reinterpret_cast<HMENU>(IDC_DUPLICATE), inst, nullptr);
            data->btnOk = CreateWindowExW(0, L"BUTTON", trI("save","Save").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW, 660, 620, 90, 36, hwnd, reinterpret_cast<HMENU>(IDOK), inst, nullptr);
            data->btnCancel = CreateWindowExW(0, L"BUTTON", trI("cancel","Cancel").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW, 770, 620, 90, 36, hwnd, reinterpret_cast<HMENU>(IDCANCEL), inst, nullptr);
            data->generalCtrls.push_back(data->btnFolder); data->generalCtrls.push_back(data->btnDuplicate);
            data->generalCtrls.push_back(data->btnOk); data->generalCtrls.push_back(data->btnCancel);

            HWND lblPlatform = CreateWindowExW(0, L"STATIC", trI("platform","Platform").c_str(), WS_CHILD | WS_VISIBLE, 24, 92, 140, 22, hwnd, nullptr, inst, nullptr);
            data->comboModsPlatform = CreateWindowExW(0, L"COMBOBOX", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | CBS_DROPDOWNLIST, 24, 116, 140, 200, hwnd, reinterpret_cast<HMENU>(IDC_MODS_PLATFORM), inst, nullptr);
            SendMessageW(data->comboModsPlatform, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L"Modrinth"));
            SendMessageW(data->comboModsPlatform, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L"CurseForge"));
            SendMessageW(data->comboModsPlatform, CB_SETCURSEL, 0, 0);
            HWND lblType = CreateWindowExW(0, L"STATIC", trI("type","Type").c_str(), WS_CHILD | WS_VISIBLE, 180, 92, 140, 22, hwnd, nullptr, inst, nullptr);
            data->comboModsType = CreateWindowExW(0, L"COMBOBOX", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | CBS_DROPDOWNLIST, 180, 116, 140, 200, hwnd, reinterpret_cast<HMENU>(IDC_MODS_TYPE), inst, nullptr);
            SendMessageW(data->comboModsType, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("mod","Mod").c_str()));
            SendMessageW(data->comboModsType, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("resource_pack","Resource Pack").c_str()));
            SendMessageW(data->comboModsType, CB_ADDSTRING, 0, reinterpret_cast<LPARAM>(trI("shader_pack","Shader Pack").c_str()));
            SendMessageW(data->comboModsType, CB_SETCURSEL, 0, 0);
            HWND lblSearch = CreateWindowExW(0, L"STATIC", trI("search","Search").c_str(), WS_CHILD | WS_VISIBLE, 336, 92, 340, 22, hwnd, nullptr, inst, nullptr);
            data->editModsSearch = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 336, 116, 340, 30, hwnd, reinterpret_cast<HMENU>(IDC_MODS_SEARCH), inst, nullptr);
            data->btnModsGo = CreateWindowExW(0, L"BUTTON", trI("search","Search").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 690, 116, 100, 30, hwnd, reinterpret_cast<HMENU>(IDC_MODS_GO), inst, nullptr);
            data->listMods = CreateWindowExW(0, L"LISTBOX", nullptr, WS_CHILD | WS_VISIBLE | WS_BORDER | WS_VSCROLL | LBS_NOTIFY | LBS_OWNERDRAWFIXED, 24, 158, 820, 360, hwnd, reinterpret_cast<HMENU>(IDC_MODS_LIST), inst, nullptr);
            data->btnModsInstall = CreateWindowExW(0, L"BUTTON", trI("install","Install").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 24, 540, 160, 34, hwnd, reinterpret_cast<HMENU>(IDC_MODS_INSTALL), inst, nullptr);
            data->btnModsOpen = CreateWindowExW(0, L"BUTTON", trI("open_mods_folder","Open Mods Folder").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 200, 540, 180, 34, hwnd, reinterpret_cast<HMENU>(IDC_MODS_OPEN), inst, nullptr);
            data->btnModsShowInstalled = CreateWindowExW(0, L"BUTTON", trI("installed","Installed").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 400, 540, 140, 34, hwnd, reinterpret_cast<HMENU>(IDC_MODS_SHOWINSTALLED), inst, nullptr);
            data->hModDetails = CreateWindowExW(0, L"STATIC", trI("select_mod_details","Select a mod to see details").c_str(), WS_CHILD | WS_VISIBLE | SS_LEFT | SS_NOPREFIX, 24, 580, 820, 60, hwnd, nullptr, inst, nullptr);
            SendMessageW(data->hModDetails, WM_SETFONT, reinterpret_cast<WPARAM>(data->smallFont), TRUE);
            data->modsCtrls.push_back(lblPlatform); data->modsCtrls.push_back(data->comboModsPlatform);
            data->modsCtrls.push_back(lblType); data->modsCtrls.push_back(data->comboModsType);
            data->modsCtrls.push_back(lblSearch); data->modsCtrls.push_back(data->editModsSearch);
            data->modsCtrls.push_back(data->btnModsGo); data->modsCtrls.push_back(data->listMods);
            data->modsCtrls.push_back(data->btnModsInstall); data->modsCtrls.push_back(data->btnModsOpen); data->modsCtrls.push_back(data->btnModsShowInstalled); data->modsCtrls.push_back(data->hModDetails);

            data->listServers = CreateWindowExW(0, L"LISTBOX", nullptr, WS_CHILD | WS_VISIBLE | WS_BORDER | WS_VSCROLL | LBS_NOTIFY, 24, 92, 500, 500, hwnd, reinterpret_cast<HMENU>(IDC_SERV_LIST), inst, nullptr);
            HWND lblServName = CreateWindowExW(0, L"STATIC", trI("name","Name").c_str(), WS_CHILD | WS_VISIBLE, 560, 92, 300, 22, hwnd, nullptr, inst, nullptr);
            data->editServName = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 560, 116, 300, 30, hwnd, reinterpret_cast<HMENU>(IDC_SERV_NAME), inst, nullptr);
            HWND lblServAddr = CreateWindowExW(0, L"STATIC", trI("address","Address (ip:port)").c_str(), WS_CHILD | WS_VISIBLE, 560, 160, 300, 22, hwnd, nullptr, inst, nullptr);
            data->editServAddr = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL, 560, 184, 300, 30, hwnd, reinterpret_cast<HMENU>(IDC_SERV_ADDR), inst, nullptr);
            data->btnServAdd = CreateWindowExW(0, L"BUTTON", trI("add_server","Add").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 560, 224, 140, 32, hwnd, reinterpret_cast<HMENU>(IDC_SERV_ADD), inst, nullptr);
            data->btnServDel = CreateWindowExW(0, L"BUTTON", trI("remove_server","Remove").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 712, 224, 140, 32, hwnd, reinterpret_cast<HMENU>(IDC_SERV_DEL), inst, nullptr);
            data->btnServOpen = CreateWindowExW(0, L"BUTTON", trI("open_folder","Open Folder").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 560, 300, 300, 32, hwnd, reinterpret_cast<HMENU>(IDC_SERV_OPEN), inst, nullptr);
            data->serversCtrls.push_back(data->listServers); data->serversCtrls.push_back(lblServName);
            data->serversCtrls.push_back(data->editServName); data->serversCtrls.push_back(lblServAddr);
            data->serversCtrls.push_back(data->editServAddr); data->serversCtrls.push_back(data->btnServAdd);
            data->serversCtrls.push_back(data->btnServDel); data->serversCtrls.push_back(data->btnServOpen);
            data->servers = loadServers(instanceDir(fromUtf8(data->cfg.name)));
            for (const Server& s : data->servers) {
                std::wstring item = fromUtf8(s.name.empty() ? s.address : s.name + " (" + s.address + ")");
                SendMessageW(data->listServers, LB_ADDSTRING, 0, reinterpret_cast<LPARAM>(item.c_str()));
            }

            HWND notesIco = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_ICON, 24, 92, 20, 20, hwnd, nullptr, inst, nullptr);
            HICON hNotesIco = loadLauncherIcon(IDR_ICON_NOTES, L"notes", 20);
            if (hNotesIco) { SendMessageW(notesIco, STM_SETIMAGE, IMAGE_ICON, reinterpret_cast<LPARAM>(hNotesIco)); data->fieldIcons.push_back(hNotesIco); }
            data->notesTitleIcon = notesIco;
            HWND lblNotesTitle = CreateWindowExW(0, L"STATIC", trI("notes_label","Notes").c_str(), WS_CHILD | WS_VISIBLE, 50, 92, 300, 22, hwnd, nullptr, inst, nullptr);
            data->notesTitle = lblNotesTitle;
            HWND lblNotesHint = CreateWindowExW(0, L"STATIC", trI("notes_hint","Personal notes for this instance — saved with it").c_str(), WS_CHILD | WS_VISIBLE, 50, 112, 500, 16, hwnd, reinterpret_cast<HMENU>(IDC_NOTES_HINT), inst, nullptr);
            data->notesHint = lblNotesHint;
            HWND notesFrame = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD | WS_VISIBLE | SS_OWNERDRAW, 24, 134, 820, 470, hwnd, reinterpret_cast<HMENU>(IDC_NOTES_FRAME), inst, nullptr);
            data->notesFrame = notesFrame;
            data->editNotes = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_MULTILINE | ES_AUTOVSCROLL | WS_VSCROLL | ES_WANTRETURN, 28, 138, 812, 462, hwnd, reinterpret_cast<HMENU>(IDC_EDIT_NOTES), inst, nullptr);
            SetWindowTextW(data->editNotes, fromUtf8(data->cfg.notes).c_str());
            SendMessageW(data->editNotes, EM_SETCUEBANNER, TRUE, reinterpret_cast<LPARAM>(trI("notes_placeholder","Write your notes here... Supports multiple lines.").c_str()));
            HWND notesCnt = CreateWindowExW(0, L"STATIC", L"", WS_CHILD | WS_VISIBLE | SS_RIGHT, 24, 608, 820, 16, hwnd, reinterpret_cast<HMENU>(IDC_NOTES_COUNTER), inst, nullptr);
            data->notesCounter = notesCnt;
            {
                int len = GetWindowTextLengthW(data->editNotes);
                std::wstring prefix = trI("characters","Characters");
                std::wstring txt = prefix + L": " + std::to_wstring(len) + L" / 2000";
                SetWindowTextW(notesCnt, txt.c_str());
            }
            data->notesCtrls.push_back(notesIco); data->notesCtrls.push_back(lblNotesTitle); data->notesCtrls.push_back(lblNotesHint);
            data->notesCtrls.push_back(notesFrame); data->notesCtrls.push_back(data->editNotes); data->notesCtrls.push_back(notesCnt);

            data->listLogs = CreateWindowExW(0, L"LISTBOX", nullptr, WS_CHILD | WS_VISIBLE | WS_BORDER | WS_VSCROLL | LBS_NOTIFY, 24, 92, 420, 500, hwnd, reinterpret_cast<HMENU>(IDC_LOGS_LIST), inst, nullptr);
            data->editLogView = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_VISIBLE | WS_BORDER | ES_MULTILINE | ES_AUTOVSCROLL | ES_READONLY | WS_VSCROLL, 460, 92, 400, 500, hwnd, reinterpret_cast<HMENU>(IDC_LOGS_VIEW), inst, nullptr);
            data->btnLogsOpen = CreateWindowExW(0, L"BUTTON", trI("open_folder","Open Folder").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 24, 608, 160, 32, hwnd, reinterpret_cast<HMENU>(IDC_LOGS_OPEN), inst, nullptr);
            data->btnLogsRefresh = CreateWindowExW(0, L"BUTTON", trI("refresh","Refresh").c_str(), WS_CHILD | WS_VISIBLE | WS_TABSTOP, 200, 608, 160, 32, hwnd, reinterpret_cast<HMENU>(IDC_LOGS_REFRESH), inst, nullptr);
            data->logsCtrls.push_back(data->listLogs); data->logsCtrls.push_back(data->editLogView);
            data->logsCtrls.push_back(data->btnLogsOpen); data->logsCtrls.push_back(data->btnLogsRefresh);
            data->logFiles = loadLogFiles(instanceDir(fromUtf8(data->cfg.name)));
            for (const std::wstring& f : data->logFiles) {
                std::wstring name = f.substr(f.find_last_of(L"\\/") + 1);
                SendMessageW(data->listLogs, LB_ADDSTRING, 0, reinterpret_cast<LPARAM>(name.c_str()));
            }

            for (HWND w : data->generalCtrls) if (w) SendMessageW(w, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            for (HWND w : data->modsCtrls) if (w) SendMessageW(w, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            for (HWND w : data->serversCtrls) if (w) SendMessageW(w, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            for (HWND w : data->notesCtrls) if (w) SendMessageW(w, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            for (HWND w : data->logsCtrls) if (w) SendMessageW(w, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);
            SendMessageW(header, WM_SETFONT, reinterpret_cast<WPARAM>(data->titleFont), TRUE);
            if (data->notesHint) SendMessageW(data->notesHint, WM_SETFONT, reinterpret_cast<WPARAM>(data->smallFont), TRUE);
            if (data->notesCounter) SendMessageW(data->notesCounter, WM_SETFONT, reinterpret_cast<WPARAM>(data->smallFont), TRUE);
            if (data->notesTitle) SendMessageW(data->notesTitle, WM_SETFONT, reinterpret_cast<WPARAM>(data->font), TRUE);

            subclassWheel(data->editName); subclassWheel(data->editRam); subclassWheel(data->editJvm);
            subclassWheel(data->editJava); subclassWheel(data->comboVer);
            subclassWheel(data->comboLoader); subclassWheel(data->comboLoaderVer);
            subclassWheel(data->editModsSearch); subclassWheel(data->editServName); subclassWheel(data->editServAddr);

            setBtnIconEd(data->btnBrowse, L"folder");
            setBtnIconEd(data->btnFolder, L"folder");
            setBtnIconEd(data->btnDuplicate, L"edit");
            setBtnIconEd(data->btnOk, L"play");
            setBtnIconEd(data->btnCancel, L"delete");
            setBtnIconEd(data->btnModsGo, L"search");
            setBtnIconEd(data->btnModsInstall, L"download");
            setBtnIconEd(data->btnModsOpen, L"folder");
            setBtnIconEd(data->btnServAdd, L"add");
            setBtnIconEd(data->btnServDel, L"delete");
            setBtnIconEd(data->btnServOpen, L"folder");
            setBtnIconEd(data->btnLogsOpen, L"folder");
            setBtnIconEd(data->btnLogsRefresh, L"update");
            for (HWND b : {data->btnBrowse, data->btnFolder, data->btnDuplicate, data->btnOk, data->btnCancel, data->btnModsGo, data->btnModsInstall, data->btnModsOpen, data->btnServAdd, data->btnServDel, data->btnServOpen, data->btnLogsOpen, data->btnLogsRefresh}) if (b) {
                SetWindowTheme(b, L"Explorer", nullptr);
                LONG_PTR st = GetWindowLongPtrW(b, GWL_STYLE);
                st |= BS_FLAT;
                SetWindowLongPtrW(b, GWL_STYLE, st);
                SetWindowPos(b, nullptr, 0,0,0,0, SWP_NOMOVE|SWP_NOSIZE|SWP_NOZORDER|SWP_FRAMECHANGED);
            }
            SendMessageW(data->editNotes, EM_SETMARGINS, EC_LEFTMARGIN | EC_RIGHTMARGIN, MAKELPARAM(8, 8));
            {
                RECT rc; GetClientRect(data->editNotes, &rc); rc.left+=2; rc.right-=2; rc.top+=2; rc.bottom-=2;
                SendMessageW(data->editNotes, EM_SETRECT, 0, reinterpret_cast<LPARAM>(&rc));
            }

            glowCreate(hwnd, &data->glow);
            glowSetButtons(&data->glow, {data->btnBrowse, data->btnFolder, data->btnDuplicate, data->btnOk, data->btnCancel, data->btnModsGo, data->btnModsInstall, data->btnModsOpen, data->btnServAdd, data->btnServDel, data->btnServOpen, data->btnLogsOpen, data->btnLogsRefresh});
            applyTab(data, 0);
            SetTimer(hwnd, 99, 16, nullptr);
            SetFocus(data->editName);
            return 0;
        }
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds = reinterpret_cast<DRAWITEMSTRUCT*>(lParam);
            int id = static_cast<int>(ds->CtlID);
            if (id >= IDC_TAB && id < IDC_TAB + 5 && data) {
                int idx = id - IDC_TAB;
                bool sel = (data->curTab == idx);
                bool isPrev = (data->prevTab == idx && data->tabFade > 0);
                HDC dc = ds->hDC;
                RECT rc = ds->rcItem;
                int dpi = 96;
                if (HMODULE u = GetModuleHandleW(L"user32.dll")) {
                    auto fn = reinterpret_cast<UINT(WINAPI*)(HWND)>(GetProcAddress(u, "GetDpiForWindow"));
                    if (fn) dpi = (int)fn(hwnd);
                }
                if (dpi < 96) dpi = 96;
                auto sc = [&](int v){ return MulDiv(v, dpi, 96); };
                COLORREF bg = sel ? data->theme.accent : data->theme.panel;
                COLORREF fg = sel ? RGB(255,255,255) : data->theme.text;
                if (isPrev) {
                    float f = data->tabFade;
                    auto blend = [&](int a,int b){ return (int)(a*f + b*(1-f)); };
                    bg = RGB(blend(GetRValue(data->theme.accent), GetRValue(data->theme.panel)), blend(GetGValue(data->theme.accent), GetGValue(data->theme.panel)), blend(GetBValue(data->theme.accent), GetBValue(data->theme.panel)));
                    fg = RGB(blend(255, GetRValue(data->theme.text)), blend(255, GetGValue(data->theme.text)), blend(255, GetBValue(data->theme.text)));
                }
                if (!sel && !isPrev && (ds->itemState & ODS_SELECTED)) bg = data->theme.accent;
                HBRUSH br = CreateSolidBrush(bg);
                FillRect(dc, &rc, br);
                DeleteObject(br);
                int iconSz = sc(16);
                int pad = sc(10);
                int ix = rc.left + pad;
                int iy = rc.top + (rc.bottom - rc.top - iconSz) / 2;
                if (idx >= 0 && idx < (int)data->tabIcons.size() && data->tabIcons[idx]) DrawIconEx(dc, ix, iy, data->tabIcons[idx], iconSz, iconSz, 0, nullptr, DI_NORMAL);
                RECT tr = rc;
                tr.left = ix + iconSz + sc(6);
                tr.right -= sc(6);
                SetBkMode(dc, TRANSPARENT);
                SetTextColor(dc, fg);
                HFONT oldF = (HFONT)SelectObject(dc, data->font);
                auto trTab = [&](const char* k,const char* fb){const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
                std::wstring tabNames2[] = {trTab("general","General"), trTab("mods","Mods"), trTab("servers","Servers"), trTab("notes","Notes"), trTab("logs","Logs")};
                DrawTextW(dc, tabNames2[idx].c_str(), -1, &tr, DT_SINGLELINE | DT_VCENTER | DT_LEFT | DT_END_ELLIPSIS);
                SelectObject(dc, oldF);
                if (sel) {
                    RECT ul = rc; ul.top = ul.bottom - sc(3); ul.bottom = rc.bottom;
                    HBRUSH abr = CreateSolidBrush(RGB(255,255,255));
                    FillRect(dc, &ul, abr); DeleteObject(abr);
                }
                return TRUE;
            }
            if (id == IDC_NOTES_FRAME && data) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                HBRUSH bgBr = CreateSolidBrush(data->theme.panel);
                FillRect(dc, &rc, bgBr); DeleteObject(bgBr);
                Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                Gdiplus::Color penCol(60, 255, 255, 255); Gdiplus::Pen pen(penCol, 1.0f);
                pen.SetLineJoin(Gdiplus::LineJoinRound);
                Gdiplus::GraphicsPath path; int r=12; path.AddArc(rc.left, rc.top, r, r, 180, 90); path.AddArc(rc.right - r, rc.top, r, r, 270, 90); path.AddArc(rc.right - r, rc.bottom - r, r, r, 0, 90); path.AddArc(rc.left, rc.bottom - r, r, r, 90, 90); path.CloseFigure();
                g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                Gdiplus::SolidBrush br(Gdiplus::Color(GetRValue(data->theme.panel), GetGValue(data->theme.panel), GetBValue(data->theme.panel)));
                g.FillPath(&br, &path);
                g.DrawPath(&pen, &path);
                return TRUE;
            }
            if (id == IDC_MODS_LIST && data) {
                DRAWITEMSTRUCT* ds = reinterpret_cast<DRAWITEMSTRUCT*>(lParam);
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                int dpi = 96;
                if (HMODULE u = GetModuleHandleW(L"user32.dll")) {
                    auto fn = reinterpret_cast<UINT(WINAPI*)(HWND)>(GetProcAddress(u, "GetDpiForWindow"));
                    if (fn) dpi = (int)fn(hwnd);
                }
                if (dpi < 96) dpi = 96;
                auto sc = [&](int v){ return MulDiv(v, dpi, 96); };
                bool sel = (ds->itemState & ODS_SELECTED) != 0;
                HBRUSH br = CreateSolidBrush(sel ? data->theme.accent : data->theme.bg);
                FillRect(dc, &rc, br); DeleteObject(br);
                int idx = (int)ds->itemID;
                if (idx >= 0 && idx < (int)data->modResults.size()) {
                    const ModInfo& m = data->modResults[idx];
                    int iconSz = sc(32);
                    int pad = sc(10);
                    int iy = rc.top + (rc.bottom - rc.top - iconSz) / 2;
if (m.hIcon) DrawIconEx(dc, rc.left + pad, iy, m.hIcon, iconSz, iconSz, 0, nullptr, DI_NORMAL);
                else {
                    std::wstring fallbackPath = iconPathEd(L"mods");
                    if (fileExists(fallbackPath)) {
                        HICON hFallback = loadHIconEd(fallbackPath, iconSz);
                        if (hFallback) DrawIconEx(dc, rc.left + pad, iy, hFallback, iconSz, iconSz, 0, nullptr, DI_NORMAL);
                        DestroyIcon(hFallback);
                    } else { HBRUSH pb = CreateSolidBrush(data->theme.panel); Rectangle(dc, rc.left + pad, iy, rc.left + pad + iconSz, iy + iconSz); DeleteObject(pb); }
                }
                    RECT tr = rc; tr.left = rc.left + pad + iconSz + sc(10); tr.right -= sc(8); tr.top += sc(4); tr.bottom = rc.top + (rc.bottom - rc.top) / 2;
                    SetBkMode(dc, TRANSPARENT); SetTextColor(dc, sel ? RGB(255,255,255) : RGB(255,255,255));
                    HFONT oldF = (HFONT)SelectObject(dc, data->font);
                    DrawTextW(dc, fromUtf8(m.title).c_str(), -1, &tr, DT_SINGLELINE | DT_VCENTER | DT_LEFT | DT_END_ELLIPSIS);
                    SelectObject(dc, oldF);
                    RECT dr = rc; dr.left = tr.left; dr.right = tr.right; dr.top = tr.bottom; dr.bottom = rc.bottom - sc(4);
                    HFONT oldS = (HFONT)SelectObject(dc, data->smallFont);
                    SetTextColor(dc, sel ? RGB(220,220,220) : RGB(150,150,150));
                    DrawTextW(dc, fromUtf8(m.description).c_str(), -1, &dr, DT_SINGLELINE | DT_VCENTER | DT_LEFT | DT_END_ELLIPSIS);
                    SelectObject(dc, oldS);
                }
                if (ds->itemState & ODS_FOCUS) DrawFocusRect(dc, &rc);
                return TRUE;
            }
            if ((id == IDOK || id == IDCANCEL || id == IDC_OPEN_FOLDER || id == IDC_DUPLICATE || id == IDC_BROWSE_JAVA || id == IDC_MODS_GO || id == IDC_MODS_INSTALL || id == IDC_MODS_OPEN || id == IDC_MODS_SHOWINSTALLED || id == IDC_SERV_ADD || id == IDC_SERV_DEL || id == IDC_SERV_OPEN || id == IDC_LOGS_OPEN || id == IDC_LOGS_REFRESH) && data) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool isOk = (id == IDOK);
                bool isPrimary = isOk || id == IDC_MODS_INSTALL;
                COLORREF bg = isPrimary ? data->theme.accent : data->theme.panel;
                COLORREF fg = isPrimary ? RGB(255,255,255) : data->theme.text;
                if (ds->itemState & ODS_DISABLED) { bg = RGB(54,54,58); fg = RGB(130,130,130); }
                else if (ds->itemState & ODS_SELECTED) bg = isPrimary ? RGB(70,120,235) : RGB(60,60,62);
                Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                HBRUSH bgFill=CreateSolidBrush(bg); FillRect(dc,&rc,bgFill); DeleteObject(bgFill);
                Gdiplus::Color gc(GetRValue(bg),GetGValue(bg),GetBValue(bg)); Gdiplus::SolidBrush br(gc);
                Gdiplus::GraphicsPath path; path.AddArc(rc.left, rc.top, 8,8,180,90); path.AddArc(rc.right-8, rc.top, 8,8,270,90); path.AddArc(rc.right-8, rc.bottom-8, 8,8,0,90); path.AddArc(rc.left, rc.bottom-8, 8,8,90,90); path.CloseFigure();
                g.FillPath(&br, &path);
                wchar_t txt[64]; GetWindowTextW(ds->hwndItem, txt, 64);
                Gdiplus::SolidBrush tbr(Gdiplus::Color(GetRValue(fg),GetGValue(fg),GetBValue(fg)));
                Gdiplus::Font f(dc, data->font);
                Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentCenter); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter); fmt.SetTrimming(Gdiplus::StringTrimmingEllipsisCharacter);
                Gdiplus::RectF rf((Gdiplus::REAL)rc.left, (Gdiplus::REAL)rc.top, (Gdiplus::REAL)(rc.right-rc.left), (Gdiplus::REAL)(rc.bottom-rc.top));
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                g.DrawString(txt, -1, &f, rf, &fmt, &tbr);
                return TRUE;
            }
            if (id == IDC_CHK_BUNDLED && data) {
                HDC dc = ds->hDC; RECT rc = ds->rcItem;
                bool chk = SendMessageW(data->chkBundle, BM_GETCHECK, 0, 0) == BST_CHECKED;
                HBRUSH bg = CreateSolidBrush(data->theme.bg); FillRect(dc, &rc, bg); DeleteObject(bg);
                int box = 16; int bx = rc.left + 2; int by = rc.top + (rc.bottom - rc.top - box) / 2;
                HBRUSH boxBr = CreateSolidBrush(chk ? data->theme.accent : data->theme.panel);
                HPEN boxPen = CreatePen(PS_SOLID, 1, RGB(225, 225, 225));
                HBRUSH ob = (HBRUSH)SelectObject(dc, boxBr); HPEN op = (HPEN)SelectObject(dc, boxPen);
                Rectangle(dc, bx, by, bx + box, by + box);
                SelectObject(dc, ob); SelectObject(dc, op); DeleteObject(boxBr); DeleteObject(boxPen);
                if (chk) {
                    HPEN cpen = CreatePen(PS_SOLID, 2, RGB(255,255,255));
                    HPEN oc = (HPEN)SelectObject(dc, cpen);
                    MoveToEx(dc, bx+4, by+8, nullptr); LineTo(dc, bx+7, by+12); LineTo(dc, bx+13, by+4);
                    SelectObject(dc, oc); DeleteObject(cpen);
                }
                SetBkMode(dc, TRANSPARENT); SetTextColor(dc, RGB(255,255,255));
                HFONT old = (HFONT)SelectObject(dc, data->font);
                RECT tr = {bx + box + 8, rc.top, rc.right - 4, rc.bottom};
                {
                    const char* v=ravex::lang("use_bundled_java"); if(!v||strcmp(v,"use_bundled_java")==0) v="Use bundled Java";
                    std::wstring wtxt=fromUtf8(v);
                    DrawTextW(dc, wtxt.c_str(), -1, &tr, DT_LEFT | DT_VCENTER | DT_SINGLELINE);
                }
                SelectObject(dc, old);
                return TRUE;
            }
            return FALSE;
        }
        case WM_MEASUREITEM: {
            MEASUREITEMSTRUCT* m = reinterpret_cast<MEASUREITEMSTRUCT*>(lParam);
            if (m->CtlID == IDC_MODS_LIST) { m->itemHeight = MulDiv(44, 96, 96); if (HMODULE u = GetModuleHandleW(L"user32.dll")) { auto fn = reinterpret_cast<UINT(WINAPI*)(HWND)>(GetProcAddress(u, "GetDpiForWindow")); if (fn) m->itemHeight = MulDiv(44, (int)fn(hwnd), 96); } return TRUE; }
            return FALSE;
        }
        case WM_NOTIFY: {
            NMHDR* nm = reinterpret_cast<NMHDR*>(lParam);
            if (nm->idFrom == IDC_RAM_SPIN && nm->code == UDN_DELTAPOS) {
                NMUPDOWN* ud = reinterpret_cast<NMUPDOWN*>(lParam);
                ud->iDelta = ud->iDelta > 0 ? 512 : -512;
                return 0;
            }
            return 0;
        }
        case WM_TIMER:
            if (wParam == 99) glowUpdate(&data->glow);
            else if (wParam == 100) {
                if (data->prevTab >=0) {
                    data->tabFade -= 0.06f;
                    if (data->tabFade <= 0) { data->tabFade = 0; data->prevTab = -1; KillTimer(hwnd, 100); }
                    if (data->prevTab >=0 && data->prevTab < (int)data->tabBtns.size() && data->tabBtns[data->prevTab]) InvalidateRect(data->tabBtns[data->prevTab], nullptr, TRUE);
                    if (data->curTab >=0 && data->curTab < (int)data->tabBtns.size() && data->tabBtns[data->curTab]) InvalidateRect(data->tabBtns[data->curTab], nullptr, TRUE);
                } else KillTimer(hwnd, 100);
            }
            else if (wParam == 123) { KillTimer(hwnd, 123); PostMessageW(hwnd, WM_COMMAND, MAKEWPARAM(IDC_MODS_GO, BN_CLICKED), 0); }
            return 0;
        case WM_MOUSEWHEEL: {
            if (data->maxScroll <= 0) return 0;
            int z = GET_WHEEL_DELTA_WPARAM(wParam);
            int step = (z / WHEEL_DELTA) * 40;
            int old = data->scrollPos;
            int np = data->scrollPos - step;
            if (np < 0) np = 0;
            if (np > data->maxScroll) np = data->maxScroll;
            data->scrollPos = np;
            int delta = data->scrollPos - old;
            if (delta != 0) {
                ScrollWindowEx(hwnd, 0, -delta, nullptr, nullptr, nullptr, nullptr, SW_SCROLLCHILDREN | SW_INVALIDATE | SW_ERASE);
                SetScrollPos(hwnd, SB_VERT, data->scrollPos, TRUE);
            }
            return 0;
        }
        case WM_VSCROLL: {
            int old = data->scrollPos;
            int step = 40;
            switch (LOWORD(wParam)) {
                case SB_LINEUP: data->scrollPos -= step; break;
                case SB_LINEDOWN: data->scrollPos += step; break;
                case SB_PAGEUP: data->scrollPos -= (data->maxScroll / 5); break;
                case SB_PAGEDOWN: data->scrollPos += (data->maxScroll / 5); break;
                case SB_THUMBTRACK: data->scrollPos = HIWORD(wParam); break;
                default: break;
            }
            if (data->scrollPos < 0) data->scrollPos = 0;
            if (data->scrollPos > data->maxScroll) data->scrollPos = data->maxScroll;
            int delta = data->scrollPos - old;
            if (delta != 0) {
                ScrollWindowEx(hwnd, 0, -delta, nullptr, nullptr, nullptr, nullptr, SW_SCROLLCHILDREN | SW_INVALIDATE | SW_ERASE);
                SetScrollPos(hwnd, SB_VERT, data->scrollPos, TRUE);
            }
            return 0;
        }
        case WM_APP_MODS: {
            std::vector<ModInfo>* res = reinterpret_cast<std::vector<ModInfo>*>(lParam);
            if (!data || !IsWindow(hwnd)) { delete res; return 0; }
            if (!data->listMods || !IsWindow(data->listMods)) { delete res; return 0; }
            for (ModInfo& m : data->modResults) if (m.hIcon) { DestroyIcon(m.hIcon); m.hIcon = nullptr; }
            data->modResults = *res;
            delete res;
            SendMessageW(data->listMods, LB_RESETCONTENT, 0, 0);
            for (size_t i = 0; i < data->modResults.size(); ++i) {
                int idx = (int)SendMessageW(data->listMods, LB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L""));
                SendMessageW(data->listMods, LB_SETITEMDATA, idx, (LPARAM)i);
            }
            InvalidateRect(data->listMods, nullptr, TRUE);
            return 0;
        }
        case WM_APP + 101: {
            auto* vers = reinterpret_cast<std::vector<std::string>*>(lParam);
            if (data && data->comboVer && IsWindow(data->comboVer) && vers) {
                std::wstring cur; int l=GetWindowTextLengthW(data->comboVer); if(l>0){cur.resize(l+1); GetWindowTextW(data->comboVer,cur.data(),l+1); cur.resize(l);} 
                SendMessageW(data->comboVer, CB_RESETCONTENT, 0, 0);
                for (auto &v: *vers) SendMessageW(data->comboVer, CB_ADDSTRING, 0, (LPARAM)fromUtf8(v).c_str());
                data->versions = *vers;
                if (!cur.empty()) {
                    int idx=(int)SendMessageW(data->comboVer, CB_FINDSTRINGEXACT, (WPARAM)-1, (LPARAM)cur.c_str());
                    if(idx>=0) SendMessageW(data->comboVer, CB_SETCURSEL, idx, 0); else SendMessageW(data->comboVer, CB_SETCURSEL, 0, 0);
                } else SendMessageW(data->comboVer, CB_SETCURSEL, 0, 0);
            }
            delete vers; return 0;
        }
        case WM_COMMAND: {
            int id = LOWORD(wParam);
            int code = HIWORD(wParam);
            auto trI=[&](const char* k,const char* fb){const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
            if (id >= IDC_TAB && id < IDC_TAB + 5) {
                int idx = id - IDC_TAB;
                if (idx != data->curTab) {
                    data->prevTab = data->curTab;
                    data->tabFade = 1.0f;
                    SetTimer(hwnd, 100, 16, nullptr);
                }
                data->curTab = idx;
                applyTab(data, idx);
                for (HWND b : data->tabBtns) if (b) InvalidateRect(b, nullptr, TRUE);
                InvalidateRect(hwnd, nullptr, TRUE);
                if (idx == 1 && data->modResults.empty() && SendMessageW(data->listMods, LB_GETCOUNT, 0, 0) == 0) {
                    PostMessageW(hwnd, WM_COMMAND, MAKEWPARAM(IDC_MODS_GO, BN_CLICKED), 0);
                }
                return 0;
            }
            if (id == IDC_EDIT_NOTES && code == EN_CHANGE) {
                int len = GetWindowTextLengthW(data->editNotes);
                std::wstring prefix = trI("characters","Characters");
                std::wstring txt = prefix + L": " + std::to_wstring(len) + L" / 2000";
                SetWindowTextW(data->notesCounter, txt.c_str());
                return 0;
            }
            if (id == IDC_CHK_BUNDLED) {
                BOOL chk = SendMessageW(data->chkBundle, BM_GETCHECK, 0, 0) == BST_CHECKED;
                SendMessageW(data->chkBundle, BM_SETCHECK, chk ? BST_UNCHECKED : BST_CHECKED, 0);
                InvalidateRect(data->chkBundle, nullptr, TRUE);
                return 0;
            }
            if (id == IDC_MODS_LIST && code == LBN_SELCHANGE) {
                int sel = static_cast<int>(SendMessageW(data->listMods, LB_GETCURSEL, 0, 0));
                if (sel >= 0 && sel < (int)data->modResults.size()) {
                    const ModInfo& m = data->modResults[sel];
                    if (m.slug.empty()) {
                        std::wstring txt = fromUtf8(m.title + " - " + m.description);
                        SetWindowTextW(data->hModDetails, txt.c_str());
                    } else {
                        SetWindowTextW(data->hModDetails, fromUtf8("Loading " + m.title + "...").c_str());
                        HWND hDetails = data->hModDetails;
                        std::thread([hDetails, m]() {
                            std::string slug = m.slug;
                            std::string body = ravex::net::httpGet("https://api.modrinth.com/v2/project/" + slug + "/version", nullptr);
                            std::string latest = "unknown";
                            std::string versions;
                            if (!body.empty()) {
                                ravex::json::Value arr = ravex::json::Value::parse(body);
                                if (!arr.isNull() && arr.size() > 0) {
                                    if (arr.at(0).has("version_number")) latest = arr.at(0).at("version_number").asString();
                                    for (size_t i = 0; i < arr.size() && i < 5; ++i) {
                                        if (i) versions += ", ";
                                        if (arr.at(i).has("version_number")) versions += arr.at(i).at("version_number").asString();
                                    }
                                }
                            }
                            std::wstring msg = fromUtf8(m.title + "\n" + m.description + "\nLatest: " + latest + "\nVersions: " + versions);
                            if (IsWindow(hDetails)) SetWindowTextW(hDetails, msg.c_str());
                        }).detach();
                    }
                }
                return 0;
            }
            if (id == IDOK) {
                if (GetFocus() == data->editModsSearch) {
                    PostMessageW(hwnd, WM_COMMAND, MAKEWPARAM(IDC_MODS_GO, BN_CLICKED), 0);
                    return 0;
                }
                std::wstring name; int len = GetWindowTextLengthW(data->editName); name.resize(static_cast<std::size_t>(len+1)); GetWindowTextW(data->editName, name.data(), len + 1); if (len>0) name.resize(len); else name.clear();
                if (name.empty()) { MessageBoxW(hwnd, trI("name_empty","Name cannot be empty").c_str(), L"RaveX Launcher", MB_ICONWARNING | MB_OK); return 0; }
                std::wstring ver; len = GetWindowTextLengthW(data->comboVer); ver.resize(static_cast<std::size_t>(len+1)); GetWindowTextW(data->comboVer, ver.data(), len + 1); if (len>0) ver.resize(len); else ver.clear();
                if (ver.empty()) { MessageBoxW(hwnd, trI("mc_version_empty","MC Version cannot be empty").c_str(), L"RaveX Launcher", MB_ICONWARNING | MB_OK); return 0; }
                int ram = 4096; { std::wstring t; len = GetWindowTextLengthW(data->editRam); t.resize(static_cast<std::size_t>(len+1)); GetWindowTextW(data->editRam, t.data(), len + 1); if (len>0) t.resize(len); else t.clear(); try { ram = std::stoi(t); } catch (...) { ram = 4096; } }
                if (ram < 1024) ram = 1024; if (ram > 32768) ram = 32768; ram = ((ram + 256) / 512) * 512;
                std::wstring jvm; len = GetWindowTextLengthW(data->editJvm); jvm.resize(static_cast<std::size_t>(len+1)); GetWindowTextW(data->editJvm, jvm.data(), len + 1); if (len>0) jvm.resize(len); else jvm.clear();
                std::wstring notes; len = GetWindowTextLengthW(data->editNotes); notes.resize(static_cast<std::size_t>(len+1)); GetWindowTextW(data->editNotes, notes.data(), len + 1); if (len>0) notes.resize(len); else notes.clear();
                std::wstring javaPath; len = GetWindowTextLengthW(data->editJava); javaPath.resize(static_cast<std::size_t>(len+1)); GetWindowTextW(data->editJava, javaPath.data(), len + 1); if (len>0) javaPath.resize(len); else javaPath.clear();
                int loaderSel = static_cast<int>(SendMessageW(data->comboLoader, CB_GETCURSEL, 0, 0));
                std::string loader = "vanilla"; if (loaderSel == 1) loader = "fabric"; else if (loaderSel == 2) loader = "forge"; else if (loaderSel == 3) loader = "quilt";
                std::string loaderVer;
                if (loader == "vanilla") loaderVer = "";
                else {
                    std::wstring lv; len = GetWindowTextLengthW(data->comboLoaderVer); lv.resize(static_cast<std::size_t>(len+1)); GetWindowTextW(data->comboLoaderVer, lv.data(), len + 1); if (len>0) lv.resize(len); else lv.clear();
                    loaderVer = (lv == trI("latest","Latest")) ? std::string() : toUtf8(lv);
                }
                data->cfg.name = toUtf8(name);
                data->cfg.mcVersion = toUtf8(ver);
                data->cfg.loader = loader;
                data->cfg.loaderVersion = loaderVer;
                data->cfg.ramMb = ram;
                data->cfg.jvmArgs = toUtf8(jvm);
                data->cfg.notes = toUtf8(notes);
                data->cfg.javaPath = toUtf8(javaPath);
                data->cfg.useBundledJava = SendMessageW(data->chkBundle, BM_GETCHECK, 0, 0) == BST_CHECKED;
                saveServers(instanceDir(fromUtf8(data->cfg.name)), data->servers);
                data->ok = true; data->closed = true;
                DestroyWindow(hwnd);
                return 0;
            }
            if (id == IDCANCEL) { data->closed = true; DestroyWindow(hwnd); return 0; }
            if (id == IDC_MODS_SEARCH && code == EN_CHANGE) { SetTimer(hwnd, 123, 150, nullptr); return 0; }
            if ((id == IDC_COMBO_VER || id == IDC_COMBO_LOADER) && code == CBN_SELCHANGE) { reloadLoaderVersions(data); return 0; }
            if (id == IDC_MODS_TYPE && code == CBN_SELCHANGE) { updateModsInstallButton(data); return 0; }
            if (id == IDC_OPEN_FOLDER || id == IDC_SERV_OPEN || id == IDC_MODS_OPEN || id == IDC_LOGS_OPEN) {
                std::wstring dir = instanceDir(fromUtf8(data->cfg.name));
                if (!fileExists(dir)) createDirs(dir);
                if (id == IDC_MODS_OPEN) dir = joinPath(dir, L"mods");
                if (id == IDC_LOGS_OPEN) dir = joinPath(dir, L"logs");
                if (!fileExists(dir)) createDirs(dir);
                ShellExecuteW(hwnd, L"open", dir.c_str(), nullptr, nullptr, SW_SHOWNORMAL);
                return 0;
            }
            if (id == IDC_BROWSE_JAVA) {
                wchar_t buf[MAX_PATH] = {0};
                OPENFILENAMEW ofn{};
                ofn.lStructSize = sizeof(ofn); ofn.hwndOwner = hwnd;
                ofn.lpstrFilter = L"Java Executable\0java.exe\0All Files\0*.*\0";
                ofn.lpstrFile = buf; ofn.nMaxFile = MAX_PATH;
                ofn.Flags = OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST;
                if (GetOpenFileNameW(&ofn)) SetWindowTextW(data->editJava, buf);
                return 0;
            }
            if (id == IDC_DUPLICATE) {
                std::wstring src = instanceDir(fromUtf8(data->cfg.name));
                if (!fileExists(src)) { MessageBoxW(hwnd, trI("source_not_found","Source instance folder not found").c_str(), L"RaveX Launcher", MB_ICONWARNING | MB_OK); return 0; }
                std::string base = data->cfg.name + " copy"; int n = 1;
                while (fileExists(instanceDir(fromUtf8(base)))) { ++n; base = data->cfg.name + " copy " + std::to_string(n); }
                std::wstring dst = instanceDir(fromUtf8(base)); createDirs(dst);
                SHFILEOPSTRUCTW op{}; op.hwnd = hwnd; op.wFunc = FO_COPY;
                std::wstring from = src; if (!from.empty() && from.back() != L'\\') from += L'\\'; from += L"*";
                std::wstring pFrom = from + L'\0'; std::wstring pTo = dst + L'\\'; pTo += L'\0';
                op.pFrom = pFrom.c_str(); op.pTo = pTo.c_str(); op.fFlags = FOF_NOCONFIRMATION | FOF_NO_UI | FOF_SILENT;
                if (SHFileOperationW(&op) == 0) { std::wstring msg = trI("instance_duplicated","Instance duplicated as ").c_str() + fromUtf8(base) + L"\""; MessageBoxW(hwnd, msg.c_str(), L"RaveX Launcher", MB_ICONINFORMATION | MB_OK); }
                else MessageBoxW(hwnd, trI("duplicate_failed","Failed to duplicate instance").c_str(), L"RaveX Launcher", MB_ICONWARNING | MB_OK);
                return 0;
            }
            if (id == IDC_MODS_GO) {
                std::wstring q; int len = GetWindowTextLengthW(data->editModsSearch); q.resize(static_cast<std::size_t>(len+1)); GetWindowTextW(data->editModsSearch, q.data(), len + 1); if (len>0) q.resize(len); else q.clear();
                int typeSel = static_cast<int>(SendMessageW(data->comboModsType, CB_GETCURSEL, 0, 0));
                std::string type = "mod"; if (typeSel == 1) type = "resourcepack"; else if (typeSel == 2) type = "shader";
                int platSel = static_cast<int>(SendMessageW(data->comboModsPlatform, CB_GETCURSEL, 0, 0));
                std::wstring mv; int ml = GetWindowTextLengthW(data->comboVer); mv.resize(static_cast<std::size_t>(ml+1)); GetWindowTextW(data->comboVer, mv.data(), ml + 1); if (ml>0) mv.resize(ml); else mv.clear();
                std::string q8 = toUtf8(q); std::string mc8 = toUtf8(mv);
                HWND par = hwnd;
                std::thread([par, q8, mc8, type, platSel]() {
                    std::vector<ModInfo> res;
                    try {
                        if (platSel == 1) res = curseforgeSearch(q8, mc8, type);
                        else res = modrinthSearch(q8, mc8, type);
                        parallelLoadIcons(res, 32);
                    } catch (...) {}
                    if (!IsWindow(par)) return;
                    PostMessageW(par, WM_APP_MODS, 0, reinterpret_cast<LPARAM>(new std::vector<ModInfo>(res)));
                }).detach();
                return 0;
            }
            if (id == IDC_MODS_LIST && code == LBN_DBLCLK) { goto install_mod; }
            if (id == IDC_MODS_LIST && code == LBN_SELCHANGE) {
                int sel = static_cast<int>(SendMessageW(data->listMods, LB_GETCURSEL, 0, 0));
                if (sel >= 0 && sel < (int)data->modResults.size()) {
                    const ModInfo& m = data->modResults[sel];
                    if (m.slug.empty()) {
                        std::wstring txt = fromUtf8(m.title + " - " + m.description);
                        SetWindowTextW(data->hModDetails, txt.c_str());
                    } else {
                        SetWindowTextW(data->hModDetails, fromUtf8("Loading " + m.title + "...").c_str());
                        HWND hDetails = data->hModDetails;
                        std::thread([hDetails, m]() {
                            std::string slug = m.slug;
                            std::string body = ravex::net::httpGet("https://api.modrinth.com/v2/project/" + slug + "/version", nullptr);
                            std::string latest = "unknown";
                            std::string versions;
                            if (!body.empty()) {
                                ravex::json::Value arr = ravex::json::Value::parse(body);
                                if (!arr.isNull() && arr.size() > 0) {
                                    if (arr.at(0).has("version_number")) latest = arr.at(0).at("version_number").asString();
                                    for (size_t i = 0; i < arr.size() && i < 5; ++i) {
                                        if (i) versions += ", ";
                                        if (arr.at(i).has("version_number")) versions += arr.at(i).at("version_number").asString();
                                    }
                                }
                            }
                            std::wstring msg = fromUtf8(m.title + "\r\n" + m.description + "\r\nLatest: " + latest + "\r\nVersions: " + versions);
                            if (IsWindow(hDetails)) SetWindowTextW(hDetails, msg.c_str());
                        }).detach();
                    }
                }
                return 0;
            }
            if (id == IDC_MODS_INSTALL) {
            install_mod:
                int loaderSel2 = static_cast<int>(SendMessageW(data->comboLoader, CB_GETCURSEL, 0, 0));
                std::string loader2 = "vanilla"; if (loaderSel2 == 1) loader2 = "fabric"; else if (loaderSel2 == 2) loader2 = "forge"; else if (loaderSel2 == 3) loader2 = "quilt";
                int typeSel2 = static_cast<int>(SendMessageW(data->comboModsType, CB_GETCURSEL, 0, 0));
                if (loader2 == "vanilla" && (typeSel2 == 0 || typeSel2 == 2)) { MessageBoxW(hwnd, trI("cannot_install_vanilla","Cannot install mods/shaders on Vanilla loader").c_str(), L"RaveX", MB_ICONWARNING | MB_OK); return 0; }
                int sel = static_cast<int>(SendMessageW(data->listMods, LB_GETCURSEL, 0, 0));
                if (sel < 0 || sel >= static_cast<int>(data->modResults.size())) { MessageBoxW(hwnd, trI("select_mod_first","Select a mod first").c_str(), L"RaveX Launcher", MB_ICONWARNING | MB_OK); return 0; }
                std::wstring mv; int ml = GetWindowTextLengthW(data->comboVer); mv.resize(static_cast<std::size_t>(ml+1)); GetWindowTextW(data->comboVer, mv.data(), ml + 1); if (ml>0) mv.resize(ml); else mv.clear();
                std::wstring modsDir = joinPath(instanceDir(fromUtf8(data->cfg.name)), L"mods");
                createDirs(modsDir);
                std::string err;
                if (modrinthInstall(data->modResults[sel].slug, toUtf8(mv), modsDir, &err)) MessageBoxW(hwnd, (L"Installed: " + fromUtf8(data->modResults[sel].title)).c_str(), L"RaveX Launcher", MB_ICONINFORMATION | MB_OK);
                else MessageBoxW(hwnd, fromUtf8(err).c_str(), L"RaveX Launcher", MB_ICONWARNING | MB_OK);
                return 0;
            }
            if (id == IDC_MODS_SHOWINSTALLED) {
                std::wstring modsDir = joinPath(instanceDir(fromUtf8(data->cfg.name)), L"mods");
                createDirs(modsDir);
                for (ModInfo& m : data->modResults) if (m.hIcon) { DestroyIcon(m.hIcon); m.hIcon = nullptr; }
                data->modResults.clear();
                SendMessageW(data->listMods, LB_RESETCONTENT, 0, 0);
                std::wstring pattern = joinPath(modsDir, L"*.jar");
                WIN32_FIND_DATAW fd; HANDLE h = FindFirstFileW(pattern.c_str(), &fd);
                if (h != INVALID_HANDLE_VALUE) {
                    do {
                        if (!(fd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY)) {
                            ModInfo m;
                            m.title = toUtf8(fd.cFileName);
                            m.description = lang("installed");
                            m.slug = "";
                            m.hIcon = loadLauncherIcon(IDR_ICON_PACKAGE, L"package", 32);
                            data->modResults.push_back(std::move(m));
                            int idx = (int)SendMessageW(data->listMods, LB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L""));
                            SendMessageW(data->listMods, LB_SETITEMDATA, idx, (LPARAM)(data->modResults.size()-1));
                        }
                    } while (FindNextFileW(h, &fd));
                    FindClose(h);
                }
                if (data->modResults.empty()) {
                    ModInfo m; m.title = lang("no_installed_mods"); m.description = lang("install_mods_first");
                    data->modResults.push_back(std::move(m));
                    SendMessageW(data->listMods, LB_ADDSTRING, 0, reinterpret_cast<LPARAM>(L""));
                    SendMessageW(data->listMods, LB_SETITEMDATA, 0, 0);
                }
                InvalidateRect(data->listMods, nullptr, TRUE);
                return 0;
            }
            if (id == IDC_SERV_ADD) {
                std::wstring nm; int nl = GetWindowTextLengthW(data->editServName); nm.resize(static_cast<std::size_t>(nl+1)); GetWindowTextW(data->editServName, nm.data(), nl + 1); if (nl>0) nm.resize(nl); else nm.clear();
                std::wstring ad; int al = GetWindowTextLengthW(data->editServAddr); ad.resize(static_cast<std::size_t>(al+1)); GetWindowTextW(data->editServAddr, ad.data(), al + 1); if (al>0) ad.resize(al); else ad.clear();
                if (ad.empty()) { MessageBoxW(hwnd, trI("address_empty","Address cannot be empty").c_str(), L"RaveX Launcher", MB_ICONWARNING | MB_OK); return 0; }
                Server s; s.name = toUtf8(nm); s.address = toUtf8(ad);
                data->servers.push_back(s);
                std::wstring item = (nm.empty() ? ad : nm + L" (" + ad + L")");
                SendMessageW(data->listServers, LB_ADDSTRING, 0, reinterpret_cast<LPARAM>(item.c_str()));
                SetWindowTextW(data->editServName, L""); SetWindowTextW(data->editServAddr, L"");
                return 0;
            }
            if (id == IDC_SERV_DEL) {
                int sel = static_cast<int>(SendMessageW(data->listServers, LB_GETCURSEL, 0, 0));
                if (sel >= 0 && sel < static_cast<int>(data->servers.size())) {
                    data->servers.erase(data->servers.begin() + sel);
                    SendMessageW(data->listServers, LB_DELETESTRING, sel, 0);
                }
                return 0;
            }
            if (id == IDC_LOGS_LIST && code == LBN_SELCHANGE) {
                int sel = static_cast<int>(SendMessageW(data->listLogs, LB_GETCURSEL, 0, 0));
                if (sel >= 0 && sel < static_cast<int>(data->logFiles.size())) {
                    std::string content; readFile(data->logFiles[sel], content);
                    SetWindowTextW(data->editLogView, fromUtf8(content).c_str());
                }
                return 0;
            }
            if (id == IDC_LOGS_REFRESH) {
                SendMessageW(data->listLogs, LB_RESETCONTENT, 0, 0);
                data->logFiles = loadLogFiles(instanceDir(fromUtf8(data->cfg.name)));
                for (const std::wstring& f : data->logFiles) {
                    std::wstring name = f.substr(f.find_last_of(L"\\/") + 1);
                    SendMessageW(data->listLogs, LB_ADDSTRING, 0, reinterpret_cast<LPARAM>(name.c_str()));
                }
                return 0;
            }
            return 0;
        }
        case WM_CONTEXTMENU: {
            auto trI=[&](const char* k,const char* fb){const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
            HWND hWnd = reinterpret_cast<HWND>(wParam);
            if (data && hWnd == data->listMods) {
                int sel = static_cast<int>(SendMessageW(data->listMods, LB_GETCURSEL, 0, 0));
                if (sel < 0 || sel >= static_cast<int>(data->modResults.size())) return 0;
                HMENU hMenu = CreatePopupMenu();
                AppendMenuW(hMenu, MF_STRING, 1, trI("information","Information").c_str());
                AppendMenuW(hMenu, MF_STRING, 2, trI("delete","Delete").c_str());
                AppendMenuW(hMenu, MF_STRING, 3, trI("rename","Rename").c_str());
                POINT pt; GetCursorPos(&pt);
                int cmd = TrackPopupMenu(hMenu, TPM_RETURNCMD | TPM_NONOTIFY, pt.x, pt.y, 0, hwnd, nullptr);
                DestroyMenu(hMenu);
                if (cmd == 1) {
                    SendMessageW(hwnd, WM_COMMAND, MAKEWPARAM(IDC_MODS_LIST, LBN_SELCHANGE), 0);
                } else if (cmd == 2) {
                    const ModInfo& m = data->modResults[sel];
                    if (m.slug.empty()) {
                        std::wstring modsDir = joinPath(instanceDir(fromUtf8(data->cfg.name)), L"mods");
                        std::wstring file = joinPath(modsDir, fromUtf8(m.title));
                        if (MessageBoxW(hwnd, (trI("delete","Delete") + L" " + fromUtf8(m.title) + L"?").c_str(), trI("delete_mod","Delete Mod").c_str(), MB_YESNO | MB_ICONWARNING) == IDYES) {
                            DeleteFileW(file.c_str());
                            SendMessageW(hwnd, WM_COMMAND, MAKEWPARAM(IDC_MODS_SHOWINSTALLED, BN_CLICKED), 0);
                        }
                    } else {
                        MessageBoxW(hwnd, trI("cannot_delete_not_installed","Cannot delete - not installed. Use Installed view.").c_str(), L"RaveX", MB_OK);
                    }
                } else if (cmd == 3) {
                    const ModInfo& m = data->modResults[sel];
                    if (m.slug.empty()) {
                        MessageBoxW(hwnd, trI("rename_via_explorer","Rename via file explorer in mods folder").c_str(), L"RaveX", MB_OK);
                    } else {
                        MessageBoxW(hwnd, trI("cannot_rename_not_installed","Cannot rename - not installed").c_str(), L"RaveX", MB_OK);
                    }
                }
                return 0;
            }
            break;
        }
        case WM_CTLCOLORSTATIC:
        case WM_CTLCOLORBTN: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            HWND ctrl = reinterpret_cast<HWND>(lParam);
            if (!data) { SetBkColor(dc, kBg); SetTextColor(dc, RGB(255,255,255)); return reinterpret_cast<LRESULT>(GetStockObject(BLACK_BRUSH)); }
            if (ctrl == data->notesHint || ctrl == data->notesCounter) { SetBkColor(dc, data->theme.bg); SetTextColor(dc, RGB(150,150,150)); return reinterpret_cast<LRESULT>(data->bgBrush); }
            if (ctrl == data->notesTitle) { SetBkColor(dc, data->theme.bg); SetTextColor(dc, data->theme.text); return reinterpret_cast<LRESULT>(data->bgBrush); }
            SetBkColor(dc, data->theme.bg); SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(data->bgBrush);
        }
        case WM_GETMINMAXINFO: {
            MINMAXINFO* mi = reinterpret_cast<MINMAXINFO*>(lParam);
            mi->ptMinTrackSize.x = 820;
            mi->ptMinTrackSize.y = 600;
            return 0;
        }
        case WM_CTLCOLOREDIT:
        case WM_CTLCOLORLISTBOX: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            HWND ctrl = reinterpret_cast<HWND>(lParam);
            if (!data) { SetBkColor(dc, kPanel); SetTextColor(dc, RGB(255,255,255)); return reinterpret_cast<LRESULT>(GetStockObject(BLACK_BRUSH)); }
            if (ctrl == data->editNotes) { SetBkColor(dc, data->theme.panel); SetTextColor(dc, RGB(255,255,255)); return reinterpret_cast<LRESULT>(data->panelBrush); }
            SetBkColor(dc, data->theme.panel); SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(data->panelBrush);
        }
        case WM_ERASEBKGND: {
            if (!data) return 0;
            HDC dc = reinterpret_cast<HDC>(wParam);
            RECT rc; GetClientRect(hwnd, &rc); FillRect(dc, &rc, data->bgBrush);
            return 1;
        }
        case WM_CLOSE: data->closed = true; DestroyWindow(hwnd); return 0;
        case WM_DESTROY:
            if (data) data->closed = true;
            KillTimer(hwnd, 99);
            KillTimer(hwnd, 101);
            glowDestroy(&data->glow);
            DeleteObject(data->font);
            if (data->titleFont) DeleteObject(data->titleFont);
            if (data && data->bgBrush) DeleteObject(data->bgBrush);
            if (data && data->panelBrush) DeleteObject(data->panelBrush);
            if (data) for (HICON ic : data->tabIcons) if (ic) DestroyIcon(ic);
            if (data) for (HICON ic : data->fieldIcons) if (ic) DestroyIcon(ic);
            if (data) for (ModInfo& m : data->modResults) if (m.hIcon) { DestroyIcon(m.hIcon); m.hIcon = nullptr; }
            if (data && data->smallFont) DeleteObject(data->smallFont);
            return 0;
        default:
            return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
    return 0;
}

}

std::vector<std::string> fetchVersions() {
    LauncherConfig lc = loadLauncherConfig();
    std::string err;
    std::string body = ravex::net::httpGet("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json", &err);
    std::vector<std::string> out;
    if (body.empty()) return out;
    ravex::json::Value root = ravex::json::Value::parse(body);
    if (root.isNull() || !root.has("versions")) return out;
    const ravex::json::Value& arr = root.at("versions");
    for (size_t i = 0; i < arr.size(); ++i) {
        const ravex::json::Value& v = arr.at(i);
        if (!v.has("id") || !v.has("type")) continue;
        std::string id = v.at("id").asString();
        std::string type = v.at("type").asString();
        if (type == "release") out.push_back(id);
        else if (type == "snapshot" && lc.showSnapshots) out.push_back(id);
        else if (type == "old_beta" && lc.showBeta) out.push_back(id);
        else if (type == "old_alpha" && lc.showAlpha) out.push_back(id);
    }
    if (out.empty()) {
        for (size_t i = 0; i < arr.size(); ++i) {
            const ravex::json::Value& v = arr.at(i);
            if (v.has("id") && v.has("type") && v.at("type").asString() == "release") out.push_back(v.at("id").asString());
        }
    }
    return out;
}

bool showInstanceEditor(HWND parent, ravex::InstanceCfg& cfg, bool isNew) {
    HINSTANCE inst = reinterpret_cast<HINSTANCE>(GetWindowLongPtrW(parent, GWLP_HINSTANCE));
    static bool registered = false;
    if (!registered) {
        WNDCLASSEXW wc{};
        wc.cbSize = sizeof(wc);
        wc.lpfnWndProc = EditorProc;
        wc.hInstance = inst;
        wc.hIcon = LoadIconW(inst, MAKEINTRESOURCEW(101));
        if (!wc.hIcon) wc.hIcon = LoadIconW(nullptr, MAKEINTRESOURCEW(32512));
        wc.hIconSm = wc.hIcon;
        wc.hCursor = LoadCursorW(nullptr, MAKEINTRESOURCEW(32512));
        wc.lpszClassName = L"RavexInstanceEditor";
        RegisterClassExW(&wc);
        registered = true;
    }
    EditorData data;
    data.cfg = cfg;
    data.versions = {cfg.mcVersion.empty() ? std::string("1.21.11") : cfg.mcVersion};
    if (data.versions.empty()) data.versions.push_back("1.21.11");
    {
        LauncherConfig lc = loadLauncherConfig();
        setCurrentLanguage(lc.language.c_str());
    }
    auto trT=[&](const char* k,const char* fb){const char* v=ravex::lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
    std::wstring title = isNew ? trT("new_instance","New Instance") : trT("edit_instance","Edit Instance");
    if (!inst) inst = GetModuleHandleW(nullptr);
    int pdpi = 96;
    if (HMODULE u = GetModuleHandleW(L"user32.dll")) {
        auto fn = reinterpret_cast<UINT(WINAPI*)(HWND)>(GetProcAddress(u, "GetDpiForWindow"));
        if (fn && parent) pdpi = (int)fn(parent);
    }
    if (pdpi < 96) pdpi = 96;
    RECT pr{}; GetWindowRect(parent, &pr);
    int pw = pr.right - pr.left; int ph = pr.bottom - pr.top;
    int dw = MulDiv(1020, pdpi, 96); int dh = MulDiv(740, pdpi, 96);
    int x = pr.left + (pw - dw) / 2;
    int y = pr.top + (ph - dh) / 2;
    HMONITOR mon = MonitorFromWindow(parent, MONITOR_DEFAULTTONEAREST);
    MONITORINFO mi{}; mi.cbSize = sizeof(mi); GetMonitorInfoW(mon, &mi);
    RECT wr = mi.rcWork;
    if (x < wr.left) x = wr.left + 16;
    if (y < wr.top) y = wr.top + 16;
    if (x + dw > wr.right) x = wr.right - dw - 16;
    if (y + dh > wr.bottom) y = wr.bottom - dh - 16;
    HWND hwnd = CreateWindowExW(0, L"RavexInstanceEditor", title.c_str(),
                                   WS_POPUP | WS_CAPTION | WS_SYSMENU | WS_THICKFRAME | WS_MAXIMIZEBOX | WS_VSCROLL,
                                   x, y, dw, dh, parent, nullptr, inst, &data);
    if (!hwnd) return false;
    {
        LauncherConfig ccfg = loadLauncherConfig();
        ThemeColors cth = getThemeForConfig(ccfg.theme, ccfg.customBg, ccfg.customPanel, ccfg.customText, ccfg.customAccent);
        applyWindowTheme(hwnd, cth);
    }
    std::thread([hwnd]{
        auto vers = fetchVersions();
        if (!vers.empty() && IsWindow(hwnd)) {
            auto* pv = new std::vector<std::string>(vers);
            PostMessageW(hwnd, WM_APP + 101, 0, (LPARAM)pv);
        }
    }).detach();
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
    RedrawWindow(parent, nullptr, nullptr, RDW_INVALIDATE | RDW_ALLCHILDREN | RDW_UPDATENOW | RDW_FRAME);
    SetForegroundWindow(parent);
    if (data.ok) cfg = data.cfg;
    return data.ok;
}

}
