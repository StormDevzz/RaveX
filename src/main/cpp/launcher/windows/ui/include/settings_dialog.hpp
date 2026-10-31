#pragma once
#include <windows.h>
namespace ravex::ui {
// posted by Settings on "Apply theme" so the launcher repaints live
constexpr UINT WM_APP_THEME_APPLIED = WM_APP + 9;
bool showSettingsDialog(HWND parent);
}
