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

}