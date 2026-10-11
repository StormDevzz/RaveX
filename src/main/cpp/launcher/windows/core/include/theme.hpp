#pragma once
#include <string>
#include <windows.h>
#include <gdiplus.h>

namespace ravex {

struct ThemeColors {
    COLORREF bg = RGB(28, 28, 30);
    COLORREF panel = RGB(40, 40, 42);
    COLORREF text = RGB(225, 225, 225);
    COLORREF accent = RGB(90, 140, 255);
    COLORREF buttonBg = RGB(90, 140, 255);
    COLORREF glow = RGB(200, 225, 255);
    bool glowEnabled = true;
    int colorAlpha = 255;
    bool isDark = true;
    // optional classic (Windows 95) styling. 0 means "use bg/text".
    COLORREF caption = 0;        // title bar fill
    COLORREF captionText = 0;    // title bar text
    bool classic = false;        // square corners + raised/sunken 3D bevels, no glow
    // frutiger Aero / Vista-7 styling: soft white glass sheen across the top
    // half of buttons. Only meaningful when "classic" is off.
    bool gloss = false;
};

ThemeColors getTheme(const std::string& name);
ThemeColors getThemeForConfig(const std::string& themeName, COLORREF customBg, COLORREF customPanel, COLORREF customText, COLORREF customAccent, COLORREF customButton = 0, int customAlpha = 255, COLORREF customGlow = 0, bool glowEnabled = true);
const char* themeDisplayName(const std::string& name);
int themeCount();
const char* themeNameByIndex(int index);
void applyWindowTheme(HWND hwnd, const ThemeColors& theme);

// paints a themed box: rounded rect for modern themes, or - when "classic" is
// set - a Windows 95 style square face with a raised/sunken 3D bevel.
// "sunken" flips the bevel (used for pressed buttons and text fields).
void paintThemedBox(Gdiplus::Graphics& g, const Gdiplus::RectF& rc, COLORREF fill, bool classic, bool sunken);

}
