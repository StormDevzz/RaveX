#include "ui/include/color_dialog.hpp"
#include "core/include/util.hpp"
#include "core/include/lang.hpp"
#include <windows.h>
#include <windowsx.h>
#include <commctrl.h>
#include <gdiplus.h>
#include <string>
#include <vector>
#include <cmath>
#include <algorithm>
namespace ravex::ui {
namespace {
struct ColorData {
    bool ok=false;
    COLORREF sel=RGB(90,140,255);
    int alpha=255;
    float hue=220, sat=0.65f, val=1.0f;
    HWND hwnd=nullptr,hPreview=nullptr,hAlpha=nullptr,hOk=nullptr,hCancel=nullptr,hSV=nullptr,hHue=nullptr;
    std::vector<COLORREF> presets;
    HFONT font=nullptr;
    HBRUSH bg=nullptr;
    bool draggingSV=false, draggingHue=false;
    HBITMAP hSvBmp=nullptr;
    float lastHue=-1;
    ULONG_PTR gdiTok=0;
};
void rgbToHsv(COLORREF c,float &h,float &s,float &v){ float r=GetRValue(c)/255.f,g=GetGValue(c)/255.f,b=GetBValue(c)/255.f; float mx=std::max(r,std::max(g,b)), mn=std::min(r,std::min(g,b)); float d=mx-mn; v=mx; s=mx==0?0:d/mx; if(d==0) h=0; else if(mx==r) h=60*fmod((g-b)/d,6); else if(mx==g) h=60*((b-r)/d+2); else h=60*((r-g)/d+4); if(h<0) h+=360; }
COLORREF hsvToRgb(float h,float s,float v){ float c=v*s, x=c*(1-fabs(fmod(h/60,2)-1)), m=v-c; float r=0,g=0,b=0; if(h<60){r=c;g=x;}else if(h<120){r=x;g=c;}else if(h<180){g=c;b=x;}else if(h<240){g=x;b=c;}else if(h<300){r=x;b=c;}else{r=c;b=x;} return RGB((BYTE)((r+m)*255),(BYTE)((g+m)*255),(BYTE)((b+m)*255)); }
void updateFromSel(ColorData* d){ rgbToHsv(d->sel,d->hue,d->sat,d->val); }
void updateSelFromHsv(ColorData* d){ d->sel=hsvToRgb(d->hue,d->sat,d->val); }
LRESULT CALLBACK ColorProc(HWND hwnd,UINT msg,WPARAM wParam,LPARAM lParam){
 ColorData* d=(ColorData*)GetWindowLongPtrW(hwnd,GWLP_USERDATA);
 switch(msg){
  case WM_CREATE:{
   CREATESTRUCTW* cs=(CREATESTRUCTW*)lParam; d=(ColorData*)cs->lpCreateParams; SetWindowLongPtrW(hwnd,GWLP_USERDATA,(LONG_PTR)d); d->hwnd=hwnd;
   auto tr=[&](const char* k,const char* fb){const char* v=lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
    d->font=CreateFontW(-13,0,0,0,FW_NORMAL,FALSE,FALSE,FALSE,DEFAULT_CHARSET,OUT_OUTLINE_PRECIS,CLIP_DEFAULT_PRECIS,CLEARTYPE_QUALITY,VARIABLE_PITCH|FF_SWISS,L"Manrope");
   if(!d->font) d->font=CreateFontW(-13,0,0,0,FW_NORMAL,FALSE,FALSE,FALSE,DEFAULT_CHARSET,OUT_OUTLINE_PRECIS,CLIP_DEFAULT_PRECIS,CLEARTYPE_QUALITY,VARIABLE_PITCH|FF_SWISS,L"Segoe UI Variable");
   d->bg=CreateSolidBrush(RGB(18,18,18));
   updateFromSel(d);
   HINSTANCE inst=cs->hInstance;
   int x=16,y=16,sz=36,gap=6;
   for(size_t i=0;i<d->presets.size();++i){ HWND b=CreateWindowExW(0,L"BUTTON",nullptr,WS_CHILD|WS_VISIBLE|BS_OWNERDRAW,x,y,sz,sz,hwnd,(HMENU)(1000+(int)i),inst,nullptr); SendMessageW(b,WM_SETFONT,(WPARAM)d->font,TRUE); x+=sz+gap; if(x+sz>400){x=16;y+=sz+gap;}}
   int py=y+sz+12;
   d->hSV=CreateWindowExW(0,L"STATIC",nullptr,WS_CHILD|WS_VISIBLE|SS_OWNERDRAW|WS_BORDER,16,py,180,180,hwnd,(HMENU)3001,inst,nullptr);
   d->hHue=CreateWindowExW(0,L"STATIC",nullptr,WS_CHILD|WS_VISIBLE|SS_OWNERDRAW|WS_BORDER,210,py,28,180,hwnd,(HMENU)3002,inst,nullptr);
   d->hPreview=CreateWindowExW(0,L"STATIC",nullptr,WS_CHILD|WS_VISIBLE|SS_OWNERDRAW|WS_BORDER,250,py,90,40,hwnd,nullptr,inst,nullptr);
   CreateWindowExW(0,L"STATIC",tr("transparency","Transparency").c_str(),WS_CHILD|WS_VISIBLE|SS_LEFT,16,py+200,200,18,hwnd,nullptr,inst,nullptr);
   d->hAlpha=CreateWindowExW(0,TRACKBAR_CLASSW,nullptr,WS_CHILD|WS_VISIBLE|TBS_NOTICKS,16,py+218,324,26,hwnd,(HMENU)2001,inst,nullptr);
   SendMessageW(d->hAlpha,TBM_SETRANGE,TRUE,MAKELONG(0,255)); SendMessageW(d->hAlpha,TBM_SETTHUMBLENGTH,18,0); SendMessageW(d->hAlpha,TBM_SETPOS,TRUE,d->alpha);
   std::wstring okTxt=tr("save","OK"); if(okTxt==L"Save") okTxt=L"OK";
    d->hOk=CreateWindowExW(0,L"BUTTON",okTxt.c_str(),WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW,170,py+252,90,30,hwnd,(HMENU)IDOK,inst,nullptr);
   std::wstring cancelTxt=tr("cancel","Cancel"); if(cancelTxt.length()>12) cancelTxt=fromUtf8(lang("cancel")?lang("cancel"):"Cancel");
   if(cancelTxt.size()>12){ cancelTxt=cancelTxt.substr(0,10)+L"..."; }
   d->hCancel=CreateWindowExW(0,L"BUTTON",cancelTxt.c_str(),WS_CHILD|WS_VISIBLE|WS_TABSTOP|BS_OWNERDRAW,270,py+252,90,30,hwnd,(HMENU)IDCANCEL,inst,nullptr);
   for(HWND c:{d->hOk,d->hCancel}) SendMessageW(c,WM_SETFONT,(WPARAM)d->font,TRUE);
   return 0;
  }
   case WM_DRAWITEM:{
    DRAWITEMSTRUCT* ds=(DRAWITEMSTRUCT*)lParam;
    if(ds->CtlID==IDOK || ds->CtlID==IDCANCEL){
     HDC dc=ds->hDC; RECT rc=ds->rcItem;
     bool isPrimary=(ds->CtlID==IDOK);
     COLORREF bg=isPrimary?RGB(90,140,255):RGB(45,45,47);
     if(ds->itemState&ODS_SELECTED) bg=isPrimary?RGB(70,120,235):RGB(60,60,62);
     else if(ds->itemState&ODS_HOTLIGHT) bg=isPrimary?RGB(100,150,255):RGB(55,55,57);
     HBRUSH bgBr=CreateSolidBrush(bg); FillRect(dc,&rc,bgBr); DeleteObject(bgBr);
     Gdiplus::Graphics g(dc); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
     Gdiplus::SolidBrush br(Gdiplus::Color(GetRValue(bg),GetGValue(bg),GetBValue(bg)));
     Gdiplus::GraphicsPath path; path.AddArc(rc.left,rc.top,8,8,180,90); path.AddArc(rc.right-8,rc.top,8,8,270,90); path.AddArc(rc.right-8,rc.bottom-8,8,8,0,90); path.AddArc(rc.left,rc.bottom-8,8,8,90,90); path.CloseFigure();
     g.FillPath(&br,&path);
     wchar_t txt[64]; GetWindowTextW(ds->hwndItem,txt,64);
     if(wcslen(txt)>0){
      Gdiplus::SolidBrush tbr(Gdiplus::Color(255,255,255));
      Gdiplus::Font f(dc,d->font);
      Gdiplus::StringFormat fmt; fmt.SetAlignment(Gdiplus::StringAlignmentCenter); fmt.SetLineAlignment(Gdiplus::StringAlignmentCenter);
      Gdiplus::RectF rf((Gdiplus::REAL)rc.left,(Gdiplus::REAL)rc.top,(Gdiplus::REAL)(rc.right-rc.left),(Gdiplus::REAL)(rc.bottom-rc.top));
      g.SetTextRenderingHint(Gdiplus::TextRenderingHintClearTypeGridFit);
      g.DrawString(txt,-1,&f,rf,&fmt,&tbr);
     }
     return TRUE;
    }
    if(ds->CtlID>=1000 && ds->CtlID<1000+(int)d->presets.size()){
    COLORREF c=d->presets[ds->CtlID-1000]; HBRUSH b=CreateSolidBrush(c); FillRect(ds->hDC,&ds->rcItem,b); DeleteObject(b);
    if(c==d->sel){ HPEN p=CreatePen(PS_SOLID,2,RGB(255,255,255)); HPEN op=(HPEN)SelectObject(ds->hDC,p); HBRUSH ob=(HBRUSH)SelectObject(ds->hDC,GetStockObject(NULL_BRUSH)); Rectangle(ds->hDC,ds->rcItem.left,ds->rcItem.top,ds->rcItem.right,ds->rcItem.bottom); SelectObject(ds->hDC,ob); SelectObject(ds->hDC,op); DeleteObject(p); }
    return TRUE;
   }
   if(ds->CtlID==3001){
     if(!d->hSvBmp || fabs(d->lastHue - d->hue) > 0.5f){
      if(d->hSvBmp) DeleteObject(d->hSvBmp);
      BITMAPINFO bi{}; bi.bmiHeader.biSize=sizeof(BITMAPINFOHEADER); bi.bmiHeader.biWidth=180; bi.bmiHeader.biHeight=-180; bi.bmiHeader.biPlanes=1; bi.bmiHeader.biBitCount=32; bi.bmiHeader.biCompression=BI_RGB;
      void* bits=nullptr; HDC hdc=GetDC(nullptr); d->hSvBmp=CreateDIBSection(hdc,&bi,DIB_RGB_COLORS,&bits,nullptr,0); ReleaseDC(nullptr,hdc);
      if(bits){ BYTE* px=(BYTE*)bits; for(int y=0;y<180;++y) for(int x=0;x<180;++x){ float s=x/179.f, v=1 - y/179.f; COLORREF c=hsvToRgb(d->hue,s,v); int o=(y*180+x)*4; px[o+0]=GetBValue(c); px[o+1]=GetGValue(c); px[o+2]=GetRValue(c); px[o+3]=255; } }
      d->lastHue=d->hue;
     }
     HDC mdc=CreateCompatibleDC(ds->hDC); HGDIOBJ old=SelectObject(mdc,d->hSvBmp); BitBlt(ds->hDC,0,0,180,180,mdc,0,0,SRCCOPY); SelectObject(mdc,old); DeleteDC(mdc);
     int sx=(int)(d->sat*179), sy=(int)((1-d->val)*179);
     Gdiplus::Graphics g(ds->hDC); g.SetSmoothingMode(Gdiplus::SmoothingModeHighQuality);
     Gdiplus::Pen pen(Gdiplus::Color(255,255,255),1.5f); g.DrawEllipse(&pen, sx-4, sy-4, 8, 8); Gdiplus::Pen pen2(Gdiplus::Color(0,0,0),1.0f); g.DrawEllipse(&pen2, sx-4, sy-4, 8, 8);
     return TRUE;
    }
    if(ds->CtlID==3002){
     Gdiplus::Graphics g(ds->hDC);
     for(int y=0;y<180;++y){ float h=y/180.f*360; COLORREF c=hsvToRgb(h,1,1); Gdiplus::SolidBrush br(Gdiplus::Color(GetRValue(c),GetGValue(c),GetBValue(c))); g.FillRectangle(&br, 0, y, 28, 1); }
     int hy=(int)(d->hue/360*179);
     Gdiplus::Pen pen(Gdiplus::Color(255,255,255),2); g.DrawLine(&pen, 0, hy, 28, hy);
     return TRUE;
    }
   if(ds->CtlID==0 && d->hPreview){
    Gdiplus::Graphics g(ds->hDC);
    Gdiplus::Color col(GetRValue(d->sel),GetGValue(d->sel),GetBValue(d->sel)); if(d->alpha<255) col=Gdiplus::Color(d->alpha,GetRValue(d->sel),GetGValue(d->sel),GetBValue(d->sel));
    Gdiplus::SolidBrush br(col);
    // checker for alpha
    if(d->alpha<255){ for(int y=0;y<40;y+=8) for(int x=0;x<90;x+=8){ bool dark=((x/8+y/8)%2)==0; Gdiplus::SolidBrush cb(dark?Gdiplus::Color(200,200,200):Gdiplus::Color(255,255,255)); g.FillRectangle(&cb,x,y,8,8);} }
    g.FillRectangle(&br,0,0,90,40);
    return TRUE;
   }
   return FALSE;
  }
  case WM_LBUTTONDOWN:{
   POINT pt{GET_X_LPARAM(lParam),GET_Y_LPARAM(lParam)};
   HWND sv=GetDlgItem(hwnd,3001), hue=GetDlgItem(hwnd,3002);
   RECT r; GetWindowRect(sv,&r); MapWindowPoints(nullptr,hwnd,(POINT*)&r,2); if(PtInRect(&r,pt)){ d->draggingSV=true; SetCapture(hwnd); }
   GetWindowRect(hue,&r); MapWindowPoints(nullptr,hwnd,(POINT*)&r,2); if(PtInRect(&r,pt)){ d->draggingHue=true; SetCapture(hwnd); }
   // fall through to move
  }
  case WM_MOUSEMOVE:{
   if(!d) return 0;
   if(d->draggingSV || d->draggingHue){
    POINT pt{GET_X_LPARAM(lParam),GET_Y_LPARAM(lParam)};
    if(d->draggingSV){
     RECT r; GetWindowRect(GetDlgItem(hwnd,3001),&r); MapWindowPoints(nullptr,hwnd,(POINT*)&r,2);
     int x=pt.x - r.left, y=pt.y - r.top;
     if(x<0) x=0; if(x>179) x=179; if(y<0) y=0; if(y>179) y=179;
     d->sat=x/179.f; d->val=1 - y/179.f; updateSelFromHsv(d); InvalidateRect(GetDlgItem(hwnd,3001),nullptr,TRUE); InvalidateRect(d->hPreview,nullptr,TRUE);
    }
    if(d->draggingHue){
     RECT r; GetWindowRect(GetDlgItem(hwnd,3002),&r); MapWindowPoints(nullptr,hwnd,(POINT*)&r,2);
     int y=pt.y - r.top; if(y<0) y=0; if(y>179) y=179; d->hue=y/179.f*360; updateSelFromHsv(d); InvalidateRect(GetDlgItem(hwnd,3001),nullptr,TRUE); InvalidateRect(GetDlgItem(hwnd,3002),nullptr,TRUE); InvalidateRect(d->hPreview,nullptr,TRUE);
    }
    return 0;
   }
   break;
  }
  case WM_LBUTTONUP:{ d->draggingSV=false; d->draggingHue=false; ReleaseCapture(); return 0; }
  case WM_COMMAND:{
   int id=LOWORD(wParam);
   if(id>=1000 && id<1000+(int)d->presets.size()){ d->sel=d->presets[id-1000]; updateFromSel(d); InvalidateRect(d->hPreview,nullptr,TRUE); InvalidateRect(GetDlgItem(hwnd,3001),nullptr,TRUE); InvalidateRect(GetDlgItem(hwnd,3002),nullptr,TRUE); for(int i=1000;i<1000+(int)d->presets.size();++i) InvalidateRect(GetDlgItem(hwnd,i),nullptr,TRUE); return 0; }
   if(id==2001){ d->alpha=(int)SendMessageW(d->hAlpha,TBM_GETPOS,0,0); InvalidateRect(d->hPreview,nullptr,TRUE); return 0; }
   if(id==IDOK){ d->ok=true; DestroyWindow(hwnd); return 0; }
   if(id==IDCANCEL){ DestroyWindow(hwnd); return 0; }
   return 0;
  }
  case WM_CTLCOLORSTATIC:{ HDC dc=(HDC)wParam; SetBkColor(dc,RGB(18,18,18)); SetTextColor(dc,RGB(240,240,240)); return (LRESULT)d->bg; }
  case WM_ERASEBKGND:{ HDC dc=(HDC)wParam; RECT rc; GetClientRect(hwnd,&rc); FillRect(dc,&rc,d->bg); return 1; }
  case WM_CLOSE: DestroyWindow(hwnd); return 0;
  case WM_DESTROY: if(d->font) DeleteObject(d->font); if(d->bg) DeleteObject(d->bg); if(d->hSvBmp) DeleteObject(d->hSvBmp); return 0;
  default: return DefWindowProcW(hwnd,msg,wParam,lParam);
 }
 return DefWindowProcW(hwnd,msg,wParam,lParam);
}
}
COLORREF showColorDialog(HWND parent,COLORREF init,int* alpha){
 InitCommonControls();
 ColorData d; d.sel=init; d.alpha=alpha?*alpha:255; d.presets={RGB(90,140,255),RGB(110,86,207),RGB(206,91,219),RGB(232,93,78),RGB(242,163,60),RGB(76,175,80),RGB(45,212,191),RGB(56,189,248),RGB(148,163,184),RGB(28,28,30),RGB(245,245,245),RGB(244,114,182),RGB(251,113,133),RGB(52,211,153),RGB(251,191,36),RGB(167,139,250)};
 HINSTANCE inst=(HINSTANCE)GetWindowLongPtrW(parent,GWLP_HINSTANCE); if(!inst) inst=GetModuleHandleW(nullptr);
 static bool reg=false;
 if(!reg){ WNDCLASSEXW wc{}; wc.cbSize=sizeof(wc); wc.lpfnWndProc=ColorProc; wc.hInstance=inst; wc.hIcon=LoadIconW(inst,MAKEINTRESOURCEW(101)); if(!wc.hIcon) wc.hIcon=LoadIconW(nullptr,MAKEINTRESOURCEW(32512)); wc.hIconSm=wc.hIcon; wc.hCursor=LoadCursorW(nullptr,MAKEINTRESOURCEW(32512)); wc.hbrBackground=(HBRUSH)(COLOR_WINDOW+1); wc.lpszClassName=L"KickxColorDlg"; RegisterClassExW(&wc); reg=true; }
 auto tr=[&](const char* k,const char* fb){const char* v=lang(k); if(!v||strcmp(v,k)==0) v=fb; return fromUtf8(v);};
 std::wstring title=tr("color_title","Choose color"); if(title.size()>20) title=title.substr(0,18)+L"...";
 int w=400,h=500; RECT pr{}; GetWindowRect(parent,&pr); int x=pr.left+(pr.right-pr.left-w)/2; int y=pr.top+(pr.bottom-pr.top-h)/2;
 HWND hwnd=CreateWindowExW(0,L"KickxColorDlg",title.c_str(),WS_POPUP|WS_CAPTION|WS_SYSMENU,x,y,w,h,parent,nullptr,inst,&d);
 if(!hwnd) return init; ShowWindow(hwnd,SW_SHOW); UpdateWindow(hwnd); EnableWindow(parent,FALSE);
 MSG msg; while(!d.ok && IsWindow(hwnd)){ if(PeekMessageW(&msg,nullptr,0,0,PM_REMOVE)){ if(msg.message==WM_QUIT){ PostMessageW(nullptr,WM_QUIT,0,0); break; } if(!IsWindow(hwnd)) break; if(IsDialogMessageW(hwnd,&msg)) continue; TranslateMessage(&msg); DispatchMessageW(&msg);} else Sleep(10); }
 EnableWindow(parent,TRUE); SetForegroundWindow(parent); if(d.ok){ if(alpha) *alpha=d.alpha; return d.sel; } return init;
}
}
