#include "core/include/theme.hpp"
#include <windows.h>
#include <dwmapi.h>

#ifndef DWMWA_USE_IMMERSIVE_DARK_MODE
#define DWMWA_USE_IMMERSIVE_DARK_MODE 20
#endif
#ifndef DWMWA_CAPTION_COLOR
#define DWMWA_CAPTION_COLOR 35
#endif
#ifndef DWMWA_TEXT_COLOR
#define DWMWA_TEXT_COLOR 36
#endif
#ifndef DWMWA_BORDER_COLOR
#define DWMWA_BORDER_COLOR 34
#endif

namespace ravex {

static const char* kThemeNames[] = { "dark", "light", "win95", "vista" };
static const char* kThemeDisplay[] = { "Dark", "Light", "Windows 95", "Windows Vista / 7" };
static constexpr int kThemeCount = 4;

ThemeColors getTheme(const std::string& name) {
    if (name == "dark") return ThemeColors{RGB(18, 18, 18), RGB(30, 30, 30), RGB(240, 240, 240), RGB(90, 140, 255), RGB(45, 45, 47), RGB(200, 225, 255), true, 255, true};
    if (name == "light") return ThemeColors{RGB(232, 232, 235), RGB(248, 248, 250), RGB(18, 18, 20), RGB(0, 104, 216), RGB(0, 104, 216), RGB(30, 30, 30), false, 255, false};
    if (name == "midnight") return ThemeColors{RGB(10, 14, 28), RGB(18, 24, 48), RGB(210, 220, 255), RGB(120, 80, 255), RGB(120, 80, 255), RGB(160, 180, 255), true, 255, true};
    if (name == "ocean") return ThemeColors{RGB(15, 25, 40), RGB(22, 38, 60), RGB(200, 220, 240), RGB(40, 160, 220), RGB(40, 160, 220), RGB(120, 200, 255), true, 255, true};
    if (name == "forest") return ThemeColors{RGB(18, 30, 18), RGB(28, 48, 28), RGB(210, 230, 210), RGB(80, 180, 80), RGB(80, 180, 80), RGB(120, 220, 120), true, 255, true};
    if (name == "win95") {
        // classic Windows 95: teal desktop, silver face, black text, navy title bar
        // and selection, square corners, 3D bevels, no glow.
        ThemeColors t{RGB(0, 128, 128), RGB(192, 192, 192), RGB(0, 0, 0), RGB(0, 0, 128), RGB(192, 192, 192), RGB(255, 255, 255), false, 255, false};
        t.caption = RGB(0, 0, 128);
        t.captionText = RGB(255, 255, 255);
        t.classic = true;
        return t;
    }
    if (name == "vista") {
        // frutiger Aero (Windows Vista / 7): deep glass blue, bright aero cyan
        // accents, glossy sheen on buttons. No cursor glow for this theme.
        ThemeColors t{RGB(18, 46, 78), RGB(38, 76, 116), RGB(235, 246, 255), RGB(66, 176, 255), RGB(52, 120, 190), RGB(140, 225, 255), false, 255, true};
        t.caption = RGB(24, 60, 96);
        t.captionText = RGB(255, 255, 255);
        t.gloss = true;
        return t;
    }
    return ThemeColors{RGB(18, 18, 18), RGB(30, 30, 30), RGB(240, 240, 240), RGB(90, 140, 255), RGB(45, 45, 47), RGB(200, 225, 255), true, 255, true};
}

ThemeColors getThemeForConfig(const std::string& themeName, COLORREF customBg, COLORREF customPanel, COLORREF customText, COLORREF customAccent, COLORREF customButton, int customAlpha, COLORREF customGlow, bool glowEnabled) {
    ThemeColors t = getTheme(themeName);
    if (themeName == "custom") {
        t.bg = customBg; t.panel = customPanel; t.text = customText; t.accent = customAccent;
        t.buttonBg = customButton ? customButton : customAccent;
        t.glow = customGlow ? customGlow : RGB(200, 225, 255);
        t.glowEnabled = glowEnabled;
        t.colorAlpha = customAlpha;
        t.isDark = true;
    } else {
        t.glowEnabled = t.isDark && t.glowEnabled;  // some dark themes opt out (e.g. Vista/7)
    }
    if (!t.isDark) t.glowEnabled = false;
    return t;
}

const char* themeDisplayName(const std::string& name) {
    for (int i = 0; i < kThemeCount; ++i) {
        if (name == kThemeNames[i]) return kThemeDisplay[i];
    }
    return "Dark";
}

int themeCount() { return kThemeCount; }

const char* themeNameByIndex(int index) {
    if (index >= 0 && index < kThemeCount) return kThemeNames[index];
    return "dark";
}

void applyWindowTheme(HWND hwnd, const ThemeColors& theme) {
    if (!hwnd) return;
    BOOL dark = theme.isDark ? TRUE : FALSE;
    DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, &dark, sizeof(dark));
    COLORREF cap = theme.caption ? theme.caption : theme.bg;
    COLORREF txt = theme.captionText ? theme.captionText : theme.text;
    COLORREF brd = theme.classic ? RGB(192, 192, 192) : theme.panel;
    DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, &cap, sizeof(cap));
    DwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, &txt, sizeof(txt));
    DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, &brd, sizeof(brd));
    RedrawWindow(hwnd, nullptr, nullptr, RDW_ALLCHILDREN | RDW_UPDATENOW | RDW_INVALIDATE | RDW_FRAME);
}

void paintThemedBox(Gdiplus::Graphics& g, const Gdiplus::RectF& rc, COLORREF fill, bool classic, bool sunken) {
    if (rc.Width <= 0 || rc.Height <= 0) return;
    Gdiplus::Color c(GetRValue(fill), GetGValue(fill), GetBValue(fill));
    Gdiplus::SolidBrush br(c);
    if (!classic) {
        const Gdiplus::REAL r = 8.0f;
        Gdiplus::GraphicsPath path;
        path.AddArc(rc.X, rc.Y, r * 2, r * 2, 180, 90);
        path.AddArc(rc.GetRight() - r * 2, rc.Y, r * 2, r * 2, 270, 90);
        path.AddArc(rc.GetRight() - r * 2, rc.GetBottom() - r * 2, r * 2, r * 2, 0, 90);
        path.AddArc(rc.X, rc.GetBottom() - r * 2, r * 2, r * 2, 90, 90);
        path.CloseFigure();
        Gdiplus::GraphicsState st = g.Save();
        g.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias);
        g.FillPath(&br, &path);
        g.Restore(st);
        return;
    }
    // windows 95: square face, white highlight on top/left, gray shadow on
    // bottom/right - flipped around for sunken (pressed) boxes.
    Gdiplus::GraphicsState st = g.Save();
    g.SetSmoothingMode(Gdiplus::SmoothingModeNone);
    g.FillRectangle(&br, rc);
    Gdiplus::Color hi(255, 255, 255);
    Gdiplus::Color lo(128, 128, 128);
    Gdiplus::Pen pH(sunken ? lo : hi, 1.0f);
    Gdiplus::Pen pL(sunken ? hi : lo, 1.0f);
    Gdiplus::REAL l = rc.X, t = rc.Y, rgt = rc.GetRight() - 1.0f, b = rc.GetBottom() - 1.0f;
    g.DrawLine(&pH, l, t, rgt, t);
    g.DrawLine(&pH, l, t, l, b);
    g.DrawLine(&pL, l, b, rgt, b);
    g.DrawLine(&pL, rgt, t, rgt, b);
    g.Restore(st);
}

}
