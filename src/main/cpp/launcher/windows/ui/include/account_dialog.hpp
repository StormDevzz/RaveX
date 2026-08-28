#pragma once
#include <windows.h>
#include <string>
#include "core/include/config.hpp"

namespace ravex::ui {

bool showOfflineAccountDialog(HWND parent, std::string& outName);
bool showMicrosoftAccountDialog(HWND parent, ravex::Account* out, std::string* error);

}
