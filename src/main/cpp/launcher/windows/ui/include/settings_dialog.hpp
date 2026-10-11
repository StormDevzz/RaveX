#pragma once
#include <windows.h>
namespace ravex::ui {
// posted to the main window when "Apply theme" is pressed in Settings, so the
// whole launcher repaints with the new theme without closing the dialog.
constexpr UINT WM_APP_THEME_APPLIED = WM_APP + 9;
bool showSettingsDialog(HWND parent);
}
