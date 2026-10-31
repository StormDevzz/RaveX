#pragma once
#include <string>

namespace ravex {

std::string toUtf8(const std::wstring& text);
std::wstring fromUtf8(const std::string& text);
bool writeFileAtomic(const std::wstring& path, const std::string& data);
bool readFile(const std::wstring& path, std::string& out);
bool fileExists(const std::wstring& path);
bool createDirs(const std::wstring& path);
std::wstring joinPath(const std::wstring& a, const std::wstring& b);
std::wstring windowsVersionName();
bool isLegacyWindows();
bool extractZip(const std::wstring& zipPath, const std::wstring& destDir, std::string* error);

}