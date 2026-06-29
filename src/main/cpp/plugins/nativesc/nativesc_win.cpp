#include "include/nativesc.hpp"

#ifdef _WIN32
#include <windows.h>
#include <vector>
#include <string>

namespace ravex {
namespace nativesc {

static HBITMAP createBitmapFromWindow(HWND hwnd, int& w, int& h) {
    RECT rect;
    GetClientRect(hwnd, &rect);
    w = rect.right - rect.left;
    h = rect.bottom - rect.top;

    HDC hdcScreen = GetDC(hwnd);
    HDC hdcMem = CreateCompatibleDC(hdcScreen);
    HBITMAP hBitmap = CreateCompatibleBitmap(hdcScreen, w, h);
    SelectObject(hdcMem, hBitmap);
    PrintWindow(hwnd, hdcMem, PW_CLIENTONLY);
    DeleteDC(hdcMem);
    ReleaseDC(hwnd, hdcScreen);
    return hBitmap;
}

CaptureResult captureWindow(HWND hwnd) {
    CaptureResult result;
    result.success = false;
    result.channels = 3;

    int w, h;
    HBITMAP hBitmap = createBitmapFromWindow(hwnd, w, h);
    if (!hBitmap) return result;

    HDC hdcMem = CreateCompatibleDC(nullptr);
    SelectObject(hdcMem, hBitmap);

    BITMAPINFOHEADER bi = {0};
    bi.biSize = sizeof(BITMAPINFOHEADER);
    bi.biWidth = w;
    bi.biHeight = -h;
    bi.biPlanes = 1;
    bi.biBitCount = 24;
    bi.biCompression = BI_RGB;

    std::vector<unsigned char> raw(w * h * 3);
    GetDIBits(hdcMem, hBitmap, 0, h, raw.data(), (BITMAPINFO*)&bi, DIB_RGB_COLORS);

    result.width = w;
    result.height = h;
    encodePng(raw, w, h, 3, result.data);
    result.success = true;

    DeleteDC(hdcMem);
    DeleteObject(hBitmap);
    return result;
}

}
}
#endif
