#pragma once
#include <windows.h>

namespace ravex::ui {

COLORREF showColorDialog(HWND parent, COLORREF init, int* alpha);

}
