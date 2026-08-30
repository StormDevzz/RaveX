#include "ui/include/glow.hpp"
#include <cmath>
namespace ravex::ui {
void glowCreate(HWND parent, GlowData* data) {
    if (data->bmp) DeleteObject(data->bmp);
    const int sz = 160;
    const int cx = sz / 2;
    const int cy = sz / 2;
    const float radius = 75.0f;
    void* pvBits = nullptr;
    BITMAPINFO bi{};
    bi.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    bi.bmiHeader.biWidth = sz;
    bi.bmiHeader.biHeight = -sz;
    bi.bmiHeader.biPlanes = 1;
    bi.bmiHeader.biBitCount = 32;
    bi.bmiHeader.biCompression = BI_RGB;
    HDC hdcScreen = GetDC(nullptr);
    data->bmp = CreateDIBSection(hdcScreen, &bi, DIB_RGB_COLORS, &pvBits, nullptr, 0);
    ReleaseDC(nullptr, hdcScreen);
    if (!pvBits) return;
    BYTE* pixels = static_cast<BYTE*>(pvBits);
    for (int y = 0; y < sz; ++y) {
        for (int x = 0; x < sz; ++x) {
            float dx = static_cast<float>(x - cx);
            float dy = static_cast<float>(y - cy);
            float dist = std::sqrt(dx * dx + dy * dy);
            float t = dist / radius;
            if (t > 1.0f) t = 1.0f;
            float s = 1.0f - t;
            float alpha = s * s * s * 90.0f;
            BYTE a = static_cast<BYTE>(alpha > 255.0f ? 255.0f : alpha);
            float f = a / 255.0f;
            int off = (y * sz + x) * 4;
            pixels[off + 0] = static_cast<BYTE>(200.0f * f);
            pixels[off + 1] = static_cast<BYTE>(225.0f * f);
            pixels[off + 2] = static_cast<BYTE>(255.0f * f);
            pixels[off + 3] = a;
        }
    }
    if (!data->hGlow) {
        data->hGlow = CreateWindowExW(WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_TOPMOST | WS_EX_TOOLWINDOW,
            L"Static", nullptr, WS_POPUP, 0, 0, sz, sz, parent, nullptr, GetModuleHandleW(nullptr), nullptr);
    }
    if (data->hGlow) {
        HRGN rgn = CreateEllipticRgn(3, 3, sz - 3, sz - 3);
        SetWindowRgn(data->hGlow, rgn, TRUE);
        DeleteObject(rgn);
        if (data->bmp) {
            HDC hdcSrc = CreateCompatibleDC(nullptr);
            HGDIOBJ old = SelectObject(hdcSrc, data->bmp);
            SIZE szSize = {sz, sz};
            POINT srcPt = {0, 0};
            BLENDFUNCTION blend{};
            blend.BlendOp = AC_SRC_OVER;
            blend.SourceConstantAlpha = 255;
            blend.AlphaFormat = AC_SRC_ALPHA;
            UpdateLayeredWindow(data->hGlow, nullptr, nullptr, &szSize, hdcSrc, &srcPt, 0, &blend, ULW_ALPHA);
            SelectObject(hdcSrc, old);
            DeleteDC(hdcSrc);
        }
    }
}
void glowDestroy(GlowData* data) {
    if (data->bmp) { DeleteObject(data->bmp); data->bmp = nullptr; }
    if (data->hGlow) { DestroyWindow(data->hGlow); data->hGlow = nullptr; }
    data->active = false;
}
void glowSetButtons(GlowData* data, const std::vector<HWND>& btns) {
    data->buttons = btns;
}
void glowUpdate(GlowData* data) {
    if (!data->hGlow || !data->bmp) return;
    if (!data->enabled) {
        if (data->active) { ShowWindow(data->hGlow, SW_HIDE); data->active = false; }
        data->hasPos = false;
        return;
    }
    POINT pt;
    GetCursorPos(&pt);
    HWND hit = WindowFromPoint(pt);
    bool isBtn = false;
    for (HWND b : data->buttons) if (b == hit) { isBtn = true; break; }
    if (!isBtn) {
        if (data->active) {
            ShowWindow(data->hGlow, SW_HIDE); data->active = false;
            for (HWND b : data->buttons) InvalidateRect(b, nullptr, FALSE);
        }
        data->hasPos = false;
        return;
    }
    const int sz = 160;
    int tx = pt.x - sz / 2;
    int ty = pt.y - sz / 2;
    if (!data->hasPos) { data->curX = tx; data->curY = ty; data->hasPos = true; }
    else { data->curX += static_cast<int>((tx - data->curX) * 0.72f); data->curY += static_cast<int>((ty - data->curY) * 0.72f); }
    SetWindowPos(data->hGlow, HWND_TOPMOST, data->curX, data->curY, sz, sz, SWP_NOACTIVATE | SWP_NOSIZE);
    SIZE szSize = {sz, sz};
    POINT srcPt = {0, 0};
    BLENDFUNCTION blend{};
    blend.BlendOp = AC_SRC_OVER;
    blend.SourceConstantAlpha = 255;
    blend.AlphaFormat = AC_SRC_ALPHA;
    HDC hdcSrc = CreateCompatibleDC(nullptr);
    HGDIOBJ old = SelectObject(hdcSrc, data->bmp);
    UpdateLayeredWindow(data->hGlow, nullptr, nullptr, &szSize, hdcSrc, &srcPt, 0, &blend, ULW_ALPHA);
    SelectObject(hdcSrc, old);
    DeleteDC(hdcSrc);
    if (!data->active) { ShowWindow(data->hGlow, SW_SHOWNA); data->active = true; }
}
void glowUpdateAnywhere(HWND parent, GlowData* data) {
    if (!data->hGlow || !data->bmp) return;
    if (!data->enabled) {
        if (data->active) { ShowWindow(data->hGlow, SW_HIDE); data->active = false; }
        data->hasPos = false;
        return;
    }
    POINT pt;
    GetCursorPos(&pt);
    RECT rc;
    GetWindowRect(parent, &rc);
    bool over = PtInRect(&rc, pt);
    if (!over) {
        if (data->active) {
            ShowWindow(data->hGlow, SW_HIDE); data->active = false;
            for (HWND b : data->buttons) InvalidateRect(b, nullptr, FALSE);
        }
        data->hasPos = false;
        return;
    }
    const int sz = 160;
    int tx = pt.x - sz / 2;
    int ty = pt.y - sz / 2;
    if (!data->hasPos) { data->curX = tx; data->curY = ty; data->hasPos = true; }
    else { data->curX += static_cast<int>((tx - data->curX) * 0.72f); data->curY += static_cast<int>((ty - data->curY) * 0.72f); }
    SetWindowPos(data->hGlow, HWND_TOPMOST, data->curX, data->curY, sz, sz, SWP_NOACTIVATE | SWP_NOSIZE);
    SIZE szSize = {sz, sz};
    POINT srcPt = {0, 0};
    BLENDFUNCTION blend{};
    blend.BlendOp = AC_SRC_OVER;
    blend.SourceConstantAlpha = 255;
    blend.AlphaFormat = AC_SRC_ALPHA;
    HDC hdcSrc = CreateCompatibleDC(nullptr);
    HGDIOBJ old = SelectObject(hdcSrc, data->bmp);
    UpdateLayeredWindow(data->hGlow, nullptr, nullptr, &szSize, hdcSrc, &srcPt, 0, &blend, ULW_ALPHA);
    SelectObject(hdcSrc, old);
    DeleteDC(hdcSrc);
    if (!data->active) { ShowWindow(data->hGlow, SW_SHOWNA); data->active = true; }
}
}
