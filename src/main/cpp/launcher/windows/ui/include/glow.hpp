#pragma once
#include <windows.h>
#include <vector>
namespace ravex::ui {
struct GlowData {
    HWND hGlow = nullptr;
    HBITMAP bmp = nullptr;
    bool active = false;
    bool anywhere = false;
    HWND parent = nullptr;
    std::vector<HWND> buttons;
    int curX = 0;
    int curY = 0;
    bool hasPos = false;
};
void glowCreate(HWND parent, GlowData* data);
void glowDestroy(GlowData* data);
void glowUpdate(GlowData* data);
void glowUpdateAnywhere(HWND parent, GlowData* data);
void glowSetButtons(GlowData* data, const std::vector<HWND>& btns);
}
