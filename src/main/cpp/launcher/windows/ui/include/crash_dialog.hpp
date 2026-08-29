#pragma once
#include <string>
#include <windows.h>
namespace ravex::ui {
bool showCrashDialog(HWND parent, int exitCode, const std::wstring& crashPath, const std::wstring& logPath, const std::string& reason);
}
