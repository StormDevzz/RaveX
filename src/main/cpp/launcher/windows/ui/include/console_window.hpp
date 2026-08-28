#pragma once
#include <functional>
#include <string>

namespace ravex::ui {

void openConsole(const std::function<void()>& killCallback, const std::wstring& logFile = L"", bool saveLogs = true);
void appendConsole(const std::string& line);
void closeConsole();

}
