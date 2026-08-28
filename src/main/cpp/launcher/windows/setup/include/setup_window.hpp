#pragma once
#include <windows.h>
#include <string>
namespace ravex::setup {
bool runSetupWindow(HINSTANCE inst);
bool runUninstallWindow(HINSTANCE inst);
bool isKickXInstalled(std::wstring* outPath = nullptr);
bool performUninstall(const std::wstring& installDir);
void createUninstallRegistry(const std::wstring& installDir);
void removeUninstallRegistry();
}
