#pragma once
#include <string>

namespace ravex {

std::wstring kickxDir();
std::wstring ravexDir();
std::wstring minecraftDir();
std::wstring nativesDir();
std::wstring modsDir();
std::wstring instancesDir();
std::wstring instanceDir(const std::wstring& name);
std::wstring javaPath();

// theme-specific icon sets: L"win95\\" / L"vista\\" / L"" (default icons).
// the active subdir is derived from the theme name and refreshed whenever
// launcher.json is loaded, so every window resolves icons consistently.
void setThemeIconSubdir(const std::wstring& sub);
const std::wstring& themeIconSubdir();
std::wstring themeIconSubdirFor(const std::string& themeName);
// true when an icon path points into the active theme-specific icon folder
// (such icons keep their own colors and must not be tinted).
bool isThemedIconPath(const std::wstring& path);

}