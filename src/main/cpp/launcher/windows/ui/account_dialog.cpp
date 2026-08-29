#include "ui/include/account_dialog.hpp"
#include "core/include/config.hpp"
#include "core/include/lang.hpp"
#include "core/include/theme.hpp"
#include "core/include/paths.hpp"
#include "core/include/util.hpp"
#include "ui/include/glow.hpp"
#include "net/include/http.hpp"
#include <windows.h>
#include <winhttp.h>
#include <commctrl.h>
#include <cstring>
#include <gdiplus.h>
#include <string>
#include <thread>
#include <vector>

namespace ravex::ui {

namespace {

constexpr COLORREF kBg = RGB(28, 28, 30);
constexpr COLORREF kPanel = RGB(40, 40, 42);
constexpr COLORREF kText = RGB(225, 225, 225);
constexpr COLORREF kAccent = RGB(90, 140, 255);

HFONT makeFont(int h = -13, int w = FW_NORMAL) {
    HFONT f = CreateFontW(h, 0, 0, 0, w, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       VARIABLE_PITCH | FF_SWISS, L"Manrope");
    if (!f) f = CreateFontW(h, 0, 0, 0, w, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       VARIABLE_PITCH | FF_SWISS, L"Segoe UI Variable");
    if (!f) f = CreateFontW(h, 0, 0, 0, w, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_OUTLINE_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       VARIABLE_PITCH | FF_SWISS, L"Segoe UI");
    return f;
}

HFONT makeMono(int h = -16) {
    return CreateFontW(h, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, DEFAULT_CHARSET,
                       OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, CLEARTYPE_QUALITY,
                       DEFAULT_PITCH | FF_DONTCARE, L"Consolas");
}

struct OfflineData {
    bool closed = false;
    bool ok = false;
    std::string result;
    HWND edit = nullptr;
    HWND status = nullptr;
    HFONT font = nullptr;
    HFONT smallFont = nullptr;
    GlowData glow;
    HWND okBtn = nullptr;
    HWND cancelBtn = nullptr;
    HBRUSH bgBrush = nullptr;
};

bool validOfflineName(const std::wstring& s) {
    if (s.size() < 3 || s.size() > 16) return false;
    for (wchar_t c : s) {
        if (!((c >= L'A' && c <= L'Z') || (c >= L'a' && c <= L'z') || (c >= L'0' && c <= L'9') || c == L'_')) return false;
    }
    return true;
}

LRESULT CALLBACK OfflineProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    OfflineData* d = reinterpret_cast<OfflineData*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
    switch (msg) {
        case WM_CREATE: {
            CREATESTRUCTW* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            d = reinterpret_cast<OfflineData*>(cs->lpCreateParams);
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(d));
            d->font = makeFont(-13, FW_NORMAL);
            d->smallFont = makeFont(-11, FW_NORMAL);
            d->bgBrush = CreateSolidBrush(kBg);
            HINSTANCE inst = cs->hInstance;
            {
                LauncherConfig lc = loadLauncherConfig();
                setCurrentLanguage(lc.language.c_str());
            }
            auto tr=[&](const char* k,const char* fb){const char* v=lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
            CreateWindowExW(0, L"STATIC", tr("offline_title","Create Offline Account").c_str(),
                            WS_CHILD | WS_VISIBLE | SS_LEFT,
                            20, 16, 360, 22, hwnd, nullptr, inst, nullptr);
            CreateWindowExW(0, L"STATIC", tr("offline_hint","Enter username (3-16 chars, A-Z, 0-9, _)").c_str(),
                            WS_CHILD | WS_VISIBLE | SS_LEFT,
                            20, 42, 360, 16, hwnd, nullptr, inst, nullptr);
            d->edit = CreateWindowExW(0, L"EDIT", L"",
                                      WS_CHILD | WS_VISIBLE | WS_BORDER | ES_AUTOHSCROLL,
                                      20, 62, 360, 28, hwnd, nullptr, inst, nullptr);
            d->status = CreateWindowExW(0, L"STATIC", L"",
                                        WS_CHILD | WS_VISIBLE | SS_LEFT,
                                        20, 94, 360, 16, hwnd, nullptr, inst, nullptr);
            d->okBtn = CreateWindowExW(0, L"BUTTON", tr("add","Add").c_str(),
                                         WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                         210, 122, 80, 30, hwnd, reinterpret_cast<HMENU>(IDOK), inst, nullptr);
            d->cancelBtn = CreateWindowExW(0, L"BUTTON", tr("cancel","Cancel").c_str(),
                                             WS_CHILD | WS_VISIBLE | WS_TABSTOP | BS_OWNERDRAW,
                                             300, 122, 80, 30, hwnd, reinterpret_cast<HMENU>(IDCANCEL), inst, nullptr);
            for (HWND c : {d->edit, d->okBtn, d->cancelBtn}) SendMessageW(c, WM_SETFONT, reinterpret_cast<WPARAM>(d->font), TRUE);
            SendMessageW(d->status, WM_SETFONT, reinterpret_cast<WPARAM>(d->smallFont), TRUE);
            glowCreate(hwnd, &d->glow);
            glowSetButtons(&d->glow, {d->okBtn, d->cancelBtn});
            SetTimer(hwnd, 99, 16, nullptr);
            SetFocus(d->edit);
            return 0;
        }
        case WM_COMMAND:
            if (LOWORD(wParam) == IDOK) {
                int len = GetWindowTextLengthW(d->edit);
                std::wstring t;
                t.resize(len);
                GetWindowTextW(d->edit, t.data(), len + 1);
                if (!validOfflineName(t)) {
                    SetWindowTextW(d->status, fromUtf8(lang("invalid_name")).c_str());
                    return 0;
                }
                d->result = toUtf8(t);
                d->ok = true;
                d->closed = true;
                DestroyWindow(hwnd);
                return 0;
            }
            if (LOWORD(wParam) == IDCANCEL) {
                d->closed = true;
                DestroyWindow(hwnd);
                return 0;
            }
            return 0;
        case WM_MOUSEMOVE:
            glowUpdate(&d->glow);
            return 0;
        case WM_TIMER:
            if (wParam == 99) glowUpdate(&d->glow);
            return 0;
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds=(DRAWITEMSTRUCT*)lParam;
            if(ds->CtlID==IDOK || ds->CtlID==IDCANCEL){
                HDC dc=ds->hDC; RECT rc=ds->rcItem; bool isOk=(ds->CtlID==IDOK);
                COLORREF bg=isOk?RGB(90,140,255):RGB(45,45,47); COLORREF fg=RGB(255,255,255);
                if(ds->itemState & ODS_DISABLED){ bg=RGB(60,60,60); fg=RGB(130,130,130); }
                HBRUSH bgFill=CreateSolidBrush(bg); FillRect(dc,&rc,bgFill); DeleteObject(bgFill);
                Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                Gdiplus::Color gc(GetRValue(bg),GetGValue(bg),GetBValue(bg)); Gdiplus::SolidBrush br(gc);
                Gdiplus::GraphicsPath path; path.AddArc(rc.left,rc.top,8,8,180,90); path.AddArc(rc.right-8,rc.top,8,8,270,90); path.AddArc(rc.right-8,rc.bottom-8,8,8,0,90); path.AddArc(rc.left,rc.bottom-8,8,8,90,90); path.CloseFigure();
                g.FillPath(&br,&path);
                wchar_t txt[64]; GetWindowTextW(ds->hwndItem,txt,64);
                Gdiplus::SolidBrush tbr(Gdiplus::Color(GetRValue(fg),GetGValue(fg),GetBValue(fg)));
                Gdiplus::Font f(dc,d->font); Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentCenter); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                Gdiplus::RectF rf((Gdiplus::REAL)rc.left,(Gdiplus::REAL)rc.top,(Gdiplus::REAL)(rc.right-rc.left),(Gdiplus::REAL)(rc.bottom-rc.top));
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                g.DrawString(txt,-1,&f,rf,&fmt,&tbr);
                return TRUE;
            }
            return FALSE;
        }
        case WM_CTLCOLORSTATIC: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            SetBkColor(dc, kBg);
            SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(d->bgBrush ? d->bgBrush : GetStockObject(BLACK_BRUSH));
        }
        case WM_CTLCOLOREDIT: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            HWND ctrl = reinterpret_cast<HWND>(lParam);
            if (ctrl == d->edit) {
                SetBkColor(dc, RGB(255,255,255));
                SetTextColor(dc, RGB(0,0,0));
                static HBRUSH whiteBr = nullptr; if(!whiteBr) whiteBr = CreateSolidBrush(RGB(255,255,255));
                return reinterpret_cast<LRESULT>(whiteBr);
            }
            SetBkColor(dc, kPanel);
            SetTextColor(dc, RGB(255,255,255));
            return reinterpret_cast<LRESULT>(d->bgBrush ? d->bgBrush : GetStockObject(BLACK_BRUSH));
        }
        case WM_ERASEBKGND: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            RECT rc; GetClientRect(hwnd, &rc);
            HBRUSH b = CreateSolidBrush(kBg);
            FillRect(dc, &rc, b);
            DeleteObject(b);
            return 1;
        }
        case WM_CLOSE:
            d->closed = true;
            DestroyWindow(hwnd);
            return 0;
        case WM_DESTROY:
            KillTimer(hwnd, 99);
            glowDestroy(&d->glow);
            if (d) { DeleteObject(d->font); DeleteObject(d->smallFont); if(d->bgBrush) DeleteObject(d->bgBrush); d->closed = true; }
            return 0;
        default: return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

constexpr UINT WM_MS_QR = WM_APP + 100;
constexpr UINT WM_MS_CODE = WM_APP + 101;
constexpr UINT WM_MS_STATUS = WM_APP + 102;
constexpr UINT WM_MS_DONE = WM_APP + 103;
constexpr UINT WM_MS_QR_IMAGE = WM_APP + 104;

struct MsData {
    bool closed = false;
    bool ok = false;
    ravex::Account account;
    std::string error;
    std::string userCode;
    std::string verificationUri;
    std::string statusText = "Requesting device code...";
    HWND hwnd = nullptr;
    HWND hCode = nullptr;
    HWND hLink = nullptr;
    HWND hStatus = nullptr;
    HWND hQr = nullptr;
    HWND hCancel = nullptr;
    HWND hCopy = nullptr;
    HFONT font = nullptr;
    HFONT codeFont = nullptr;
    HFONT smallFont = nullptr;
    HBITMAP hQrBmp = nullptr;
    ULONG_PTR gdiToken = 0;
    bool cancelled = false;
    GlowData glow;
    HBRUSH bgBrush = nullptr;
};

void setMsStatus(MsData* d, const std::string& s) {
    d->statusText = s;
    if (d->hStatus) SetWindowTextW(d->hStatus, fromUtf8(s).c_str());
}

std::string urlEncodeSimple(const std::string& s) {
    std::string out;
    for (unsigned char c : s) {
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-' || c == '_' || c == '.' || c == '~' || c == ':' || c == '/') out += static_cast<char>(c);
        else {
            char buf[4];
            wsprintfA(buf, "%%%02X", c);
            out += buf;
        }
    }
    return out;
}

namespace mshelper {
bool findKey(const std::string& t, const std::string& k, size_t& out) {
    std::string needle = "\"" + k + "\"";
    size_t pos = 0;
    while ((pos = t.find(needle, pos)) != std::string::npos) {
        size_t colon = pos + needle.size();
        while (colon < t.size() && (t[colon] == ' ' || t[colon] == '\t' || t[colon] == '\n' || t[colon] == '\r')) ++colon;
        if (colon < t.size() && t[colon] == ':') { out = colon + 1; return true; }
        pos = colon;
    }
    return false;
}
void skipWs(const std::string& t, size_t& i) { while (i < t.size() && (t[i] == ' ' || t[i] == '\t' || t[i] == '\n' || t[i] == '\r')) ++i; }
std::string readVal(const std::string& t, size_t& i) {
    skipWs(t,i);
    if (i >= t.size()) return "";
    if (t[i] == '"') {
        ++i; std::string o;
        while (i < t.size() && t[i] != '"') {
            if (t[i] == '\\' && i+1 < t.size()) { char e=t[i+1]; if(e=='n')o+='\n';else if(e=='r')o+='\r';else if(e=='t')o+='\t';else o+=e; i+=2; } else { o+=t[i]; ++i; }
        }
        if (i < t.size()) ++i; return o;
    }
    size_t s=i; while(i<t.size()&&t[i]!=','&&t[i]!='}'&&t[i]!=']')++i; return t.substr(s,i-s);
}
bool getString(const std::string& j, const std::string& k, std::string& o){ size_t p; if(!findKey(j,k,p))return false; o=readVal(j,p); return true; }
bool getInt(const std::string& j, const std::string& k, long long& o){ size_t p; if(!findKey(j,k,p))return false; std::string v=readVal(j,p); if(v.empty())return false; char* e=nullptr; o=strtoll(v.c_str(),&e,10); return e!=v.c_str(); }
std::string urlEncode2(const std::string& s){ std::string o; const char* h="0123456789ABCDEF"; for(unsigned char c:s){ if((c>='A'&&c<='Z')||(c>='a'&&c<='z')||(c>='0'&&c<='9')||c=='-'||c=='_'||c=='.'||c=='~')o+=c; else {o+='%'; o+=h[c>>4]; o+=h[c&15];}} return o; }
}

void msWorker(MsData* d) {
    auto postStatus = [&](const std::string& s){ std::string* p=new std::string(s); PostMessageW(d->hwnd, WM_MS_STATUS, 0, reinterpret_cast<LPARAM>(p)); };
    auto postCode = [&](const std::string& code, const std::string& uri){ std::string* c=new std::string(code); std::string* u=new std::string(uri); PostMessageW(d->hwnd, WM_MS_CODE, reinterpret_cast<WPARAM>(c), reinterpret_cast<LPARAM>(u)); };
    std::string err;
    postStatus(std::string(lang("requesting_device_code")));
    const std::string clientId = "c36a9fb6-4f2a-41ff-90bd-ae7cc92031eb";
    std::string body = "client_id=" + clientId + "&scope=" + mshelper::urlEncode2("XboxLive.signin offline_access");
    std::string resp = ravex::net::httpPost("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode", body, "application/x-www-form-urlencoded", &err);
    if (resp.empty()) { d->error = err.empty() ? std::string(lang("requesting_device_code")) : err; PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return; }
    {
        std::string e, ed;
        if (mshelper::getString(resp,"error",e) && !e.empty()) {
            mshelper::getString(resp,"error_description",ed);
            d->error = e + (ed.empty() ? "" : ": " + ed);
            if (!err.empty() && err.find("HTTP") != std::string::npos) d->error = err + " - " + d->error;
            PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return;
        }
    }
    std::string userCode, deviceCode, verificationUri, verificationComplete;
    long long expiresIn=900, interval=5;
    mshelper::getString(resp,"user_code",userCode);
    mshelper::getString(resp,"device_code",deviceCode);
    mshelper::getString(resp,"verification_uri",verificationUri);
    mshelper::getString(resp,"verification_uri_complete",verificationComplete);
    mshelper::getInt(resp,"expires_in",expiresIn);
    mshelper::getInt(resp,"interval",interval);
    if (deviceCode.empty() || userCode.empty()) { d->error = err.empty() ? "Invalid device code response" : err; PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return; }
    if (expiresIn<=0||expiresIn>900) expiresIn=900;
    if (interval<5) interval=5;
    d->userCode=userCode;
    d->verificationUri = verificationComplete.empty() ? verificationUri : verificationComplete;
    if (d->verificationUri.empty()) d->verificationUri = "https://microsoft.com/link";
    postCode(userCode, d->verificationUri);
    postStatus(std::string(lang("scan_qr")) + verificationUri + std::string(lang("enter_code")) + userCode);
    std::wstring qrUrl = fromUtf8("https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + mshelper::urlEncode2(d->verificationUri));
    // download QR png to temp
    std::wstring tmp = joinPath(joinPath(ravexDir(), L"cache"), L"qr.png");
    createDirs(joinPath(ravexDir(), L"cache"));
    std::string dlErr;
    if (ravex::net::downloadFile(toUtf8(qrUrl), tmp, nullptr, &d->cancelled, &dlErr)) {
        PostMessageW(d->hwnd, WM_MS_QR, 0, 0);
    }
    auto start = GetTickCount64();
    long long pollInterval = interval;
    std::string accessToken;
    bool authorized=false;
    while(!authorized && !d->cancelled) {
        ULONGLONG elapsed = (GetTickCount64()-start)/1000;
        if (elapsed >= (ULONGLONG)expiresIn) { d->error=std::string(lang("device_code_expired")); PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return; }
        std::string tokenBody = "grant_type=urn:ietf:params:oauth:grant-type:device_code&client_id=" + clientId + "&device_code=" + mshelper::urlEncode2(deviceCode);
        std::string tokenResp = ravex::net::httpPost("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", tokenBody, "application/x-www-form-urlencoded", &err);
        if (tokenResp.empty()) {
            if (err.find("authorization_pending") != std::string::npos) { for(int i=0;i<pollInterval*10 && !d->cancelled;++i) Sleep(100); continue; }
            if (err.find("slow_down") != std::string::npos) { pollInterval+=5; for(int i=0;i<pollInterval*10 && !d->cancelled;++i) Sleep(100); continue; }
            d->error=err.empty()?std::string(lang("token_error")):std::string(lang("token_error"))+": "+err; PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return;
        }
        std::string errCode; mshelper::getString(tokenResp,"error",errCode);
        if (errCode.empty()) {
            if(!mshelper::getString(tokenResp,"access_token",accessToken)||accessToken.empty()){ d->error="Invalid token response"; PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return; }
            authorized=true; break;
        }
        if (errCode=="authorization_pending") {}
        else if (errCode=="slow_down") pollInterval+=5;
        else         if (errCode=="authorization_declined"){ d->error=std::string(lang("auth_declined")); PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return; }
        else if (errCode=="expired_token"){ d->error=std::string(lang("device_code_expired")); PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return; }
        else { std::string desc; mshelper::getString(tokenResp,"error_description",desc); d->error=std::string(lang("token_error"))+errCode+(desc.empty()?"":" - "+desc); PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return; }
        for(int i=0;i<pollInterval*10 && !d->cancelled;++i) Sleep(100);
    }
    if (d->cancelled) { d->error=std::string(lang("cancelled")); PostMessageW(d->hwnd, WM_MS_DONE, 0, 0); return; }
    postStatus(std::string(lang("auth_xbox")));
    extern bool exchangeXbox(const std::string&, ravex::Account*, std::string*, const std::function<void(const std::string&)>&);
    // inline exchange to avoid extra file dependency: call loginMicrosoft helper via status
    // Do XBL/XSTS/MC here using same logic as accounts.cpp but simplified via net::httpPost
    std::string xblBody = "{\"RelyingParty\":\"http://auth.xboxlive.com\",\"TokenType\":\"JWT\",\"Properties\":{\"AuthMethod\":\"RPS\",\"SiteName\":\"user.auth.xboxlive.com\",\"RpsTicket\":\"d=" + accessToken + "\"}}";
    std::string xblResp = ravex::net::httpPost("https://user.auth.xboxlive.com/user/authenticate", xblBody, "application/json", &err);
    if (xblResp.empty()){ d->error=err.empty()?std::string(lang("auth_xbox_failed")):err; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
    std::string xblToken; if(!mshelper::getString(xblResp,"Token",xblToken)||xblToken.empty()){ d->error="Invalid Xbox Live response"; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
    postStatus(std::string(lang("auth_xsts")));
    std::string xstsBody="{\"RelyingParty\":\"rp://api.minecraftservices.com/\",\"TokenType\":\"JWT\",\"Properties\":{\"SandboxId\":\"RETAIL\",\"UserTokens\":[\""+xblToken+"\"]}}";
    std::string xstsResp=ravex::net::httpPost("https://xsts.auth.xboxlive.com/xsts/authorize", xstsBody, "application/json", &err);
    if (xstsResp.empty()){ d->error=err.empty()?std::string(lang("auth_xsts_failed")):err; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
    std::string xstsToken; if(!mshelper::getString(xstsResp,"Token",xstsToken)||xstsToken.empty()){ d->error="Invalid XSTS response"; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
    std::string claims, xui, uhs;
    if(mshelper::getString(xstsResp,"DisplayClaims",claims)){
        if(mshelper::getString(claims,"xui",xui)){
            size_t lb=xui.find('['); if(lb!=std::string::npos){ size_t ob=xui.find('{',lb); size_t cb=xui.find('}',ob); if(ob!=std::string::npos&&cb!=std::string::npos){ std::string first=xui.substr(ob,cb-ob+1); mshelper::getString(first,"uhs",uhs); } }
        }
    }
    if(uhs.empty()){ d->error="Invalid XSTS response (missing user hash)"; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
    postStatus(std::string(lang("requesting_mc_token")));
    std::string mcBody="{\"identityToken\":\"XBL3.0 x="+uhs+";"+xstsToken+"\"}";
    std::string mcResp=ravex::net::httpPost("https://api.minecraftservices.com/authentication/login_with_xbox", mcBody, "application/json", &err);
    if(mcResp.empty()){ d->error=err.empty()?std::string(lang("auth_mc_failed")):err; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
    std::string mcToken; if(!mshelper::getString(mcResp,"access_token",mcToken)||mcToken.empty()){ d->error="Invalid Minecraft token response"; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
    postStatus(std::string(lang("fetching_profile")));
    std::string bearerErr;
    std::string profile;
    {
        HINTERNET session = WinHttpOpen(L"kickx_launcher/1.0", WINHTTP_ACCESS_TYPE_DEFAULT_PROXY, WINHTTP_NO_PROXY_NAME, WINHTTP_NO_PROXY_BYPASS, 0);
        std::wstring wurl = fromUtf8("https://api.minecraftservices.com/minecraft/profile");
        URL_COMPONENTSW parts{}; parts.dwStructSize=sizeof(parts); wchar_t host[256]={}; wchar_t path[1024]={}; parts.lpszHostName=host; parts.dwHostNameLength=256; parts.lpszUrlPath=path; parts.dwUrlPathLength=1024;
        WinHttpCrackUrl(wurl.c_str(), (DWORD)wurl.size(), 0, &parts);
        HINTERNET conn=WinHttpConnect(session, parts.lpszHostName, parts.nPort?parts.nPort:443, 0);
        HINTERNET req=WinHttpOpenRequest(conn, L"GET", parts.lpszUrlPath, nullptr, WINHTTP_NO_REFERER, WINHTTP_DEFAULT_ACCEPT_TYPES, WINHTTP_FLAG_SECURE);
        std::wstring hdr=L"Authorization: Bearer "+fromUtf8(mcToken)+L"\r\nAccept: application/json\r\n";
        WinHttpSendRequest(req, hdr.c_str(), (DWORD)hdr.size(), WINHTTP_NO_REQUEST_DATA,0,0,0);
        WinHttpReceiveResponse(req,nullptr);
        DWORD sc=0; DWORD sl=sizeof(sc); WinHttpQueryHeaders(req, WINHTTP_QUERY_STATUS_CODE|WINHTTP_QUERY_FLAG_NUMBER, WINHTTP_HEADER_NAME_BY_INDEX, &sc, &sl, WINHTTP_NO_HEADER_INDEX);
        std::string body; DWORD av=0; do{ av=0; WinHttpQueryDataAvailable(req,&av); if(av==0)break; std::string ch(av,'\0'); DWORD rd=0; WinHttpReadData(req,ch.data(),av,&rd); body.append(ch.data(),rd);}while(av>0);
        WinHttpCloseHandle(req); WinHttpCloseHandle(conn); WinHttpCloseHandle(session);
        if(sc!=200){ d->error="Profile request failed (HTTP "+std::to_string(sc)+")"; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
        profile=body;
    }
    std::string profileId, profileName;
    if(!mshelper::getString(profile,"id",profileId)||profileId.empty()||!mshelper::getString(profile,"name",profileName)||profileName.empty()){ d->error="Invalid profile response"; PostMessageW(d->hwnd, WM_MS_DONE, 0,0); return; }
    std::string formattedUuid; formattedUuid.reserve(36); for(size_t i=0;i<profileId.size();++i){ if(i==8||i==12||i==16||i==20) formattedUuid+='-'; formattedUuid+= (char)tolower((unsigned char)profileId[i]);}
    d->account.name=profileName; d->account.uuid=formattedUuid; d->account.accessToken=mcToken; d->account.type="microsoft";
    d->ok=true;
    PostMessageW(d->hwnd, WM_MS_DONE, 1, 0);
}

HBITMAP loadQrBitmap(const std::wstring& path, int target) {
    Gdiplus::Bitmap bmp(path.c_str());
    if (bmp.GetLastStatus() != Gdiplus::Ok) return nullptr;
    Gdiplus::Bitmap* scaled = new Gdiplus::Bitmap(target, target, PixelFormat32bppARGB);
    Gdiplus::Graphics g(scaled);
    g.SetInterpolationMode(Gdiplus::InterpolationModeNearestNeighbor);
    g.DrawImage(&bmp, 0, 0, target, target);
    HBITMAP hb = nullptr;
    scaled->GetHBITMAP(Gdiplus::Color(0,0,0,0), &hb);
    delete scaled;
    return hb;
}

LRESULT CALLBACK MsProc(HWND hwnd, UINT msg, WPARAM wParam, LPARAM lParam) {
    MsData* d = reinterpret_cast<MsData*>(GetWindowLongPtrW(hwnd, GWLP_USERDATA));
    switch (msg) {
        case WM_CREATE: {
            CREATESTRUCTW* cs = reinterpret_cast<CREATESTRUCTW*>(lParam);
            d = reinterpret_cast<MsData*>(cs->lpCreateParams);
            SetWindowLongPtrW(hwnd, GWLP_USERDATA, reinterpret_cast<LONG_PTR>(d));
            d->hwnd = hwnd;
            Gdiplus::GdiplusStartupInput si; Gdiplus::GdiplusStartup(&d->gdiToken, &si, nullptr);
            d->font = makeFont(-13);
            d->smallFont = makeFont(-11);
            d->codeFont = makeMono(-18);
            d->bgBrush = CreateSolidBrush(kBg);
            HINSTANCE inst = cs->hInstance;
            {
                LauncherConfig lc = loadLauncherConfig();
                setCurrentLanguage(lc.language.c_str());
            }
            auto tr2=[&](const char* k,const char* fb){const char* v=lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
            CreateWindowExW(0, L"STATIC", tr2("ms_title","Microsoft Login").c_str(), WS_CHILD|WS_VISIBLE|SS_LEFT, 20, 16, 360, 22, hwnd, nullptr, inst, nullptr);
            d->hQr = CreateWindowExW(0, L"STATIC", nullptr, WS_CHILD|WS_VISIBLE|SS_OWNERDRAW|WS_BORDER, 94, 42, 220, 220, hwnd, nullptr, inst, nullptr);
            d->hCode = CreateWindowExW(0, L"STATIC", L"--------", WS_CHILD|WS_VISIBLE|SS_CENTER, 20, 270, 360, 36, hwnd, nullptr, inst, nullptr);
            d->hLink = CreateWindowExW(0, L"STATIC", L"https://microsoft.com/link", WS_CHILD|WS_VISIBLE|SS_CENTER, 20, 308, 360, 18, hwnd, nullptr, inst, nullptr);
            d->hStatus = CreateWindowExW(0, L"STATIC", fromUtf8(lang("requesting_device_code")).c_str(), WS_CHILD|WS_VISIBLE|SS_CENTER, 20, 332, 360, 48, hwnd, nullptr, inst, nullptr);
            d->hCopy = CreateWindowExW(0, L"BUTTON", tr2("copy_code","Copy Code").c_str(), WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW, 70, 385, 110, 28, hwnd, reinterpret_cast<HMENU>(2001), inst, nullptr);
            d->hCancel = CreateWindowExW(0, L"BUTTON", tr2("cancel","Cancel").c_str(), WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW, 220, 385, 110, 28, hwnd, reinterpret_cast<HMENU>(IDCANCEL), inst, nullptr);
            for (HWND c : {d->hCode, d->hStatus}) SendMessageW(c, WM_SETFONT, reinterpret_cast<WPARAM>(d->codeFont), TRUE);
            SendMessageW(d->hLink, WM_SETFONT, reinterpret_cast<WPARAM>(d->smallFont), TRUE);
            SendMessageW(d->hCopy, WM_SETFONT, reinterpret_cast<WPARAM>(d->font), TRUE);
            SendMessageW(d->hCancel, WM_SETFONT, reinterpret_cast<WPARAM>(d->font), TRUE);
            SendMessageW(d->hStatus, WM_SETFONT, reinterpret_cast<WPARAM>(d->smallFont), TRUE);
            glowCreate(hwnd, &d->glow);
            glowSetButtons(&d->glow, {d->hCopy, d->hCancel});
            SetTimer(hwnd, 99, 16, nullptr);
            std::thread([d]{ msWorker(d); }).detach();
            return 0;
        }
        case WM_MOUSEMOVE:
            glowUpdate(&d->glow);
            return 0;
        case WM_TIMER:
            if (wParam == 99) glowUpdate(&d->glow);
            return 0;
        case WM_DRAWITEM: {
            DRAWITEMSTRUCT* ds = reinterpret_cast<DRAWITEMSTRUCT*>(lParam);
            if (ds->CtlID == 0 && d && d->hQrBmp) {
                HDC mdc = CreateCompatibleDC(ds->hDC);
                HGDIOBJ old = SelectObject(mdc, d->hQrBmp);
                BITMAP bm{}; GetObjectW(d->hQrBmp, sizeof(bm), &bm);
                StretchBlt(ds->hDC, ds->rcItem.left, ds->rcItem.top, ds->rcItem.right - ds->rcItem.left, ds->rcItem.bottom - ds->rcItem.top, mdc, 0, 0, bm.bmWidth, bm.bmHeight, SRCCOPY);
                SelectObject(mdc, old);
                DeleteDC(mdc);
                return TRUE;
            }
            if (ds->CtlID==2001 || ds->CtlID==IDCANCEL) {
                HDC dc=ds->hDC; RECT rc=ds->rcItem; bool isOk=(ds->CtlID==2001);
                COLORREF bg=isOk?RGB(90,140,255):RGB(45,45,47); COLORREF fg=RGB(255,255,255);
                if(ds->itemState & ODS_DISABLED){ bg=RGB(60,60,60); fg=RGB(130,130,130); }
                HBRUSH bgFill=CreateSolidBrush(bg); FillRect(dc,&rc,bgFill); DeleteObject(bgFill);
                Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
                Gdiplus::Color gc(GetRValue(bg),GetGValue(bg),GetBValue(bg)); Gdiplus::SolidBrush br(gc);
                Gdiplus::GraphicsPath path; path.AddArc(rc.left,rc.top,8,8,180,90); path.AddArc(rc.right-8,rc.top,8,8,270,90); path.AddArc(rc.right-8,rc.bottom-8,8,8,0,90); path.AddArc(rc.left,rc.bottom-8,8,8,90,90); path.CloseFigure();
                g.FillPath(&br,&path);
                wchar_t txt[64]; GetWindowTextW(ds->hwndItem,txt,64);
                Gdiplus::SolidBrush tbr(Gdiplus::Color(GetRValue(fg),GetGValue(fg),GetBValue(fg)));
                Gdiplus::Font f(dc,d->font); Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentCenter); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
                Gdiplus::RectF rf((Gdiplus::REAL)rc.left,(Gdiplus::REAL)rc.top,(Gdiplus::REAL)(rc.right-rc.left),(Gdiplus::REAL)(rc.bottom-rc.top));
                g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
                g.DrawString(txt,-1,&f,rf,&fmt,&tbr);
                return TRUE;
            }
            return FALSE;
        }
        case WM_CTLCOLORSTATIC: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            HWND ctrl = reinterpret_cast<HWND>(lParam);
            if (ctrl == d->hCode) SetTextColor(dc, kAccent);
            else SetTextColor(dc, RGB(255,255,255));
            SetBkColor(dc, kBg);
            return reinterpret_cast<LRESULT>(d->bgBrush ? d->bgBrush : GetStockObject(BLACK_BRUSH));
        }
        case WM_ERASEBKGND: {
            HDC dc = reinterpret_cast<HDC>(wParam);
            RECT rc; GetClientRect(hwnd,&rc);
            HBRUSH b=CreateSolidBrush(kBg);
            FillRect(dc,&rc,b); DeleteObject(b); return 1;
        }
        case WM_COMMAND:
            if (LOWORD(wParam)==2001) {
                if(!d->userCode.empty()){
                    if(OpenClipboard(hwnd)){ EmptyClipboard(); size_t len=d->userCode.size(); HGLOBAL hg=GlobalAlloc(GMEM_MOVEABLE,(len+1)*sizeof(wchar_t)); wchar_t* p=(wchar_t*)GlobalLock(hg); std::wstring w=fromUtf8(d->userCode); wcscpy_s(p,len+1,w.c_str()); GlobalUnlock(hg); SetClipboardData(CF_UNICODETEXT,hg); CloseClipboard(); }
                    setMsStatus(d, std::string(lang("code_copied")));
                }
                return 0;
            }
            if (LOWORD(wParam)==IDCANCEL) { d->cancelled=true; d->closed=true; DestroyWindow(hwnd); return 0; }
            return 0;
        case WM_MS_CODE: {
            std::string* code = reinterpret_cast<std::string*>(wParam);
            std::string* uri = reinterpret_cast<std::string*>(lParam);
            d->userCode=*code; d->verificationUri=*uri;
            SetWindowTextW(d->hCode, fromUtf8(*code).c_str());
            SetWindowTextW(d->hLink, fromUtf8(*uri).c_str());
            delete code; delete uri;
            return 0;
        }
        case WM_MS_STATUS: {
            std::string* s = reinterpret_cast<std::string*>(lParam);
            setMsStatus(d,*s); delete s; return 0;
        }
        case WM_MS_QR: {
            std::wstring qrPath = joinPath(joinPath(ravexDir(), L"cache"), L"qr.png");
            HBITMAP hb = loadQrBitmap(qrPath, 220);
            if (hb) { if(d->hQrBmp) DeleteObject(d->hQrBmp); d->hQrBmp=hb; InvalidateRect(d->hQr,nullptr,TRUE); }
            return 0;
        }
        case WM_MS_DONE: {
            bool ok = wParam != 0;
            if (ok) { d->ok=true; d->closed=true; DestroyWindow(hwnd); }
            else { setMsStatus(d, d->error.empty() ? "Login failed" : d->error); EnableWindow(d->hCancel, TRUE); }
            return 0;
        }
        case WM_CLOSE:
            d->cancelled=true; d->closed=true; DestroyWindow(hwnd); return 0;
        case WM_DESTROY:
            KillTimer(hwnd, 99);
            glowDestroy(&d->glow);
            if(d->hQrBmp) DeleteObject(d->hQrBmp);
            if(d->gdiToken) Gdiplus::GdiplusShutdown(d->gdiToken);
            DeleteObject(d->font); DeleteObject(d->codeFont); DeleteObject(d->smallFont);
            if(d->bgBrush) DeleteObject(d->bgBrush);
            d->closed=true; return 0;
        default: return DefWindowProcW(hwnd, msg, wParam, lParam);
    }
}

}

bool showOfflineAccountDialog(HWND parent, std::string& outName) {
    HINSTANCE inst = reinterpret_cast<HINSTANCE>(GetWindowLongPtrW(parent, GWLP_HINSTANCE));
    static bool reg=false;
    if(!reg){ WNDCLASSEXW wc{}; wc.cbSize=sizeof(wc); wc.lpfnWndProc=OfflineProc; wc.hInstance=inst; wc.hIcon=LoadIconW(inst, MAKEINTRESOURCEW(101)); if(!wc.hIcon) wc.hIcon=LoadIconW(nullptr, MAKEINTRESOURCEW(32512)); wc.hIconSm=wc.hIcon; wc.hCursor=LoadCursorW(nullptr, MAKEINTRESOURCEW(32512)); wc.lpszClassName=L"KickxOfflineDlg"; RegisterClassExW(&wc); reg=true; }
    OfflineData d;
    if (!inst) inst = GetModuleHandleW(nullptr);
    RECT pr{}; GetWindowRect(parent, &pr);
    int pw = pr.right - pr.left; int ph = pr.bottom - pr.top;
    RECT cr{0,0,410,175};
    AdjustWindowRectEx(&cr, WS_POPUP|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MAXIMIZEBOX, FALSE, 0);
    int dw = cr.right - cr.left; int dh = cr.bottom - cr.top;
    int x = pr.left + (pw - dw) / 2;
    int y = pr.top + (ph - dh) / 2;
    HMONITOR mon = MonitorFromWindow(parent, MONITOR_DEFAULTTONEAREST);
    MONITORINFO mi{}; mi.cbSize = sizeof(mi); GetMonitorInfoW(mon, &mi);
    RECT wr = mi.rcWork;
    if (x < wr.left) x = wr.left + 16;
    if (y < wr.top) y = wr.top + 16;
    if (x + dw > wr.right) x = wr.right - dw - 16;
    if (y + dh > wr.bottom) y = wr.bottom - dh - 16;
    {
        LauncherConfig lc = loadLauncherConfig();
        setCurrentLanguage(lc.language.c_str());
    }
    auto trTitle=[&](const char* k,const char* fb){const char* v=lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
    HWND hwnd=CreateWindowExW(0, L"KickxOfflineDlg", trTitle("offline_title","Create Offline Account").c_str(), WS_POPUP|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MAXIMIZEBOX, x, y, dw, dh, parent, nullptr, inst, &d);
    if(!hwnd) return false;
    {
        LauncherConfig ccfg = loadLauncherConfig();
        ThemeColors cth = getThemeForConfig(ccfg.theme, ccfg.customBg, ccfg.customPanel, ccfg.customText, ccfg.customAccent);
        applyWindowTheme(hwnd, cth);
    }
    ShowWindow(hwnd, SW_SHOW);
    UpdateWindow(hwnd);
    EnableWindow(parent,FALSE);
    MSG msg; while(!d.closed){ while(PeekMessageW(&msg,nullptr,0,0,PM_REMOVE)){ if(IsDialogMessageW(hwnd,&msg))continue; TranslateMessage(&msg); DispatchMessageW(&msg);} Sleep(10); }
    EnableWindow(parent,TRUE);
    RedrawWindow(parent, nullptr, nullptr, RDW_INVALIDATE | RDW_ALLCHILDREN | RDW_UPDATENOW | RDW_FRAME);
    SetForegroundWindow(parent);
    if(d.ok) outName=d.result;
    return d.ok;
}

bool showMicrosoftAccountDialog(HWND parent, ravex::Account* out, std::string* error) {
    HINSTANCE inst = reinterpret_cast<HINSTANCE>(GetWindowLongPtrW(parent, GWLP_HINSTANCE));
    static bool reg=false;
    if(!reg){ WNDCLASSEXW wc{}; wc.cbSize=sizeof(wc); wc.lpfnWndProc=MsProc; wc.hInstance=inst; wc.hCursor=LoadCursorW(nullptr, MAKEINTRESOURCEW(32512)); wc.hIcon=LoadIconW(inst, MAKEINTRESOURCEW(101)); if(!wc.hIcon) wc.hIcon=LoadIconW(nullptr, MAKEINTRESOURCEW(32512)); wc.hIconSm=wc.hIcon; wc.lpszClassName=L"KickxMsDlg"; RegisterClassExW(&wc); reg=true; }
    MsData d;
    if (!inst) inst = GetModuleHandleW(nullptr);
    RECT pr2{}; GetWindowRect(parent, &pr2);
    int pw2 = pr2.right - pr2.left; int ph2 = pr2.bottom - pr2.top;
    RECT cr2{0,0,410,440};
    AdjustWindowRectEx(&cr2, WS_POPUP|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MAXIMIZEBOX, FALSE, 0);
    int dw2 = cr2.right - cr2.left; int dh2 = cr2.bottom - cr2.top;
    int x2 = pr2.left + (pw2 - dw2) / 2;
    int y2 = pr2.top + (ph2 - dh2) / 2;
    HMONITOR mon2 = MonitorFromWindow(parent, MONITOR_DEFAULTTONEAREST);
    MONITORINFO mi2{}; mi2.cbSize = sizeof(mi2); GetMonitorInfoW(mon2, &mi2);
    RECT wr2 = mi2.rcWork;
    if (x2 < wr2.left) x2 = wr2.left + 16;
    if (y2 < wr2.top) y2 = wr2.top + 16;
    if (x2 + dw2 > wr2.right) x2 = wr2.right - dw2 - 16;
    if (y2 + dh2 > wr2.bottom) y2 = wr2.bottom - dh2 - 16;
    {
        LauncherConfig lc = loadLauncherConfig();
        setCurrentLanguage(lc.language.c_str());
    }
    auto trMs=[&](const char* k,const char* fb){const char* v=lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
    HWND hwnd=CreateWindowExW(0, L"KickxMsDlg", trMs("ms_title","Microsoft Login").c_str(), WS_POPUP|WS_CAPTION|WS_SYSMENU|WS_THICKFRAME|WS_MAXIMIZEBOX, x2, y2, dw2, dh2, parent, nullptr, inst, &d);
    if(!hwnd) return false;
    {
        LauncherConfig ccfg = loadLauncherConfig();
        ThemeColors cth = getThemeForConfig(ccfg.theme, ccfg.customBg, ccfg.customPanel, ccfg.customText, ccfg.customAccent);
        applyWindowTheme(hwnd, cth);
    }
    ShowWindow(hwnd, SW_SHOW);
    UpdateWindow(hwnd);
    EnableWindow(parent,FALSE);
    MSG msg; while(!d.closed){ while(PeekMessageW(&msg,nullptr,0,0,PM_REMOVE)){ if(IsDialogMessageW(hwnd,&msg))continue; TranslateMessage(&msg); DispatchMessageW(&msg);} Sleep(10); }
    EnableWindow(parent,TRUE);
    RedrawWindow(parent, nullptr, nullptr, RDW_INVALIDATE | RDW_ALLCHILDREN | RDW_UPDATENOW | RDW_FRAME);
    SetForegroundWindow(parent);
    if(d.ok && out) *out=d.account;
    if(!d.ok && error) *error=d.error;
    return d.ok;
}

}
