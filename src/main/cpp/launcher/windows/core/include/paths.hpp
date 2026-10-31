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

// theme icon sets (win95/vista), refreshed on config load
void setThemeIconSubdir(const std::wstring& sub);
const std::wstring& themeIconSubdir();
std::wstring themeIconSubdirFor(const std::string& themeName);
// themed icons keep their own colors, never tint them
bool isThemedIconPath(const std::wstring& path);

}