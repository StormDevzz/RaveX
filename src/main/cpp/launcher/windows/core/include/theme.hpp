#pragma once
#include <string>
#include <windows.h>

namespace ravex {

struct ThemeColors {
    COLORREF bg = RGB(28, 28, 30);
    COLORREF panel = RGB(40, 40, 42);
    COLORREF text = RGB(225, 225, 225);
    COLORREF accent = RGB(90, 140, 255);
    COLORREF buttonBg = RGB(90, 140, 255);
    int colorAlpha = 255;
    bool isDark = true;
};

ThemeColors getTheme(const std::string& name);
ThemeColors getThemeForConfig(const std::string& themeName, COLORREF customBg, COLORREF customPanel, COLORREF customText, COLORREF customAccent, COLORREF customButton = 0, int customAlpha = 255);
const char* themeDisplayName(const std::string& name);
int themeCount();
const char* themeNameByIndex(int index);
void applyWindowTheme(HWND hwnd, const ThemeColors& theme);

}
