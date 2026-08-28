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

static const char* kThemeNames[] = { "dark", "light", "midnight", "ocean", "forest" };
static const char* kThemeDisplay[] = { "Dark", "Light", "Midnight", "Ocean", "Forest" };
static constexpr int kThemeCount = 5;

ThemeColors getTheme(const std::string& name) {
    if (name == "dark") return ThemeColors{RGB(18, 18, 18), RGB(30, 30, 30), RGB(240, 240, 240), RGB(90, 140, 255), RGB(45, 45, 47), 255, true};
    if (name == "light") return ThemeColors{RGB(242, 242, 242), RGB(255, 255, 255), RGB(32, 32, 32), RGB(0, 120, 212), RGB(0, 120, 212), 255, false};
    if (name == "midnight") return ThemeColors{RGB(10, 14, 28), RGB(18, 24, 48), RGB(210, 220, 255), RGB(120, 80, 255), RGB(120, 80, 255), 255, true};
    if (name == "ocean") return ThemeColors{RGB(15, 25, 40), RGB(22, 38, 60), RGB(200, 220, 240), RGB(40, 160, 220), RGB(40, 160, 220), 255, true};
    if (name == "forest") return ThemeColors{RGB(18, 30, 18), RGB(28, 48, 28), RGB(210, 230, 210), RGB(80, 180, 80), RGB(80, 180, 80), 255, true};
    return ThemeColors{RGB(18, 18, 18), RGB(30, 30, 30), RGB(240, 240, 240), RGB(90, 140, 255), RGB(45, 45, 47), 255, true};
}

ThemeColors getThemeForConfig(const std::string& themeName, COLORREF customBg, COLORREF customPanel, COLORREF customText, COLORREF customAccent, COLORREF customButton, int customAlpha) {
    ThemeColors t = getTheme(themeName);
    if (themeName == "custom") {
        t.bg = customBg; t.panel = customPanel; t.text = customText; t.accent = customAccent;
        t.buttonBg = customButton ? customButton : customAccent;
        t.colorAlpha = customAlpha;
        t.isDark = true;
    }
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
    COLORREF cap = theme.bg;
    COLORREF txt = theme.text;
    COLORREF brd = theme.panel;
    DwmSetWindowAttribute(hwnd, DWMWA_CAPTION_COLOR, &cap, sizeof(cap));
    DwmSetWindowAttribute(hwnd, DWMWA_TEXT_COLOR, &txt, sizeof(txt));
    DwmSetWindowAttribute(hwnd, DWMWA_BORDER_COLOR, &brd, sizeof(brd));
    RedrawWindow(hwnd, nullptr, nullptr, RDW_ALLCHILDREN | RDW_UPDATENOW | RDW_INVALIDATE | RDW_FRAME);
}

}
