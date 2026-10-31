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
    // classic windows 95 styling; 0 means "use bg/text"
    COLORREF caption = 0;
    COLORREF captionText = 0;
    bool classic = false;        // square corners, 3D bevels, no glow
    bool gloss = false;
};

ThemeColors getTheme(const std::string& name);
ThemeColors getThemeForConfig(const std::string& themeName, COLORREF customBg, COLORREF customPanel, COLORREF customText, COLORREF customAccent, COLORREF customButton = 0, int customAlpha = 255, COLORREF customGlow = 0, bool glowEnabled = true);
const char* themeDisplayName(const std::string& name);
int themeCount();
const char* themeNameByIndex(int index);
void applyWindowTheme(HWND hwnd, const ThemeColors& theme);

// color helpers for readable text on any background
COLORREF blendThemeColor(COLORREF a, COLORREF b, int weightA);
COLORREF themeContrastText(COLORREF background);
COLORREF themeMutedText(COLORREF text, COLORREF background);

// rounded rect normally, square 3D bevel for classic themes
// sunken flips the bevel (pressed buttons, text fields)
void paintThemedBox(Gdiplus::Graphics& g, const Gdiplus::RectF& rc, COLORREF fill, bool classic, bool sunken);

}
