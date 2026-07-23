#include "include/nativesc.hpp"

#ifdef _WIN32
#include <windows.h>
#else
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/XShm.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#include <cstdint>
#endif

namespace ravex {
namespace nativesc {

#ifdef _WIN32
static bool initialized = false;
#else
Display* display = nullptr;
int screen = 0;
bool hasShm = false;
#endif

bool initialize() {
#ifdef _WIN32
    initialized = true;
    return true;
#else
    display = XOpenDisplay(nullptr);
    if (!display) return false;
    screen = DefaultScreen(display);
    int major, minor;
    Bool pixmaps;
    hasShm = (XShmQueryVersion(display, &major, &minor, &pixmaps) == True);
    return true;
#endif
}

void shutdown() {
#ifndef _WIN32
    if (display) {
        XCloseDisplay(display);
        display = nullptr;
    }
#endif
}

bool encodePng(const std::vector<unsigned char>& raw, int w, int h, int channels,
                      std::vector<unsigned char>& out) {
    out.clear();
    int stride = w * channels;
    int dataSize = stride * h;

    unsigned char sig[] = {137, 80, 78, 71, 13, 10, 26, 10};
    out.insert(out.end(), sig, sig + 8);

    auto writeU32 = [&](uint32_t val) {
        out.push_back((val >> 24) & 0xFF);
        out.push_back((val >> 16) & 0xFF);
        out.push_back((val >> 8) & 0xFF);
        out.push_back(val & 0xFF);
    };

    auto crc32 = [](const unsigned char* data, size_t len) -> uint32_t {
        uint32_t crc = 0xFFFFFFFF;
        static uint32_t table[256];
        static bool tableInit = false;
        if (!tableInit) {
            for (uint32_t i = 0; i < 256; i++) {
                uint32_t c = i;
                for (int j = 0; j < 8; j++) {
                    if (c & 1) c = 0xEDB88320 ^ (c >> 1);
                    else c >>= 1;
                }
                table[i] = c;
            }
            tableInit = true;
        }
        for (size_t i = 0; i < len; i++) {
            crc = table[(crc ^ data[i]) & 0xFF] ^ (crc >> 8);
        }
        return crc ^ 0xFFFFFFFF;
    };

    size_t ihdrStart = out.size();
    out.push_back(0);
    out.push_back(0);
    out.push_back(0);
    out.push_back(13);
    out.insert(out.end(), {'I', 'H', 'D', 'R'});
    writeU32(static_cast<uint32_t>(w));
    writeU32(static_cast<uint32_t>(h));
    out.push_back(8);
    out.push_back(channels == 4 ? 6 : 2);
    out.push_back(0);
    out.push_back(0);
    out.push_back(0);
    uint32_t ihdrCrc = crc32(out.data() + ihdrStart + 4, out.size() - ihdrStart - 4);
    writeU32(ihdrCrc);

    size_t idatStart = out.size();
    std::vector<unsigned char> compressed;
    compressed.reserve(dataSize + dataSize / 10 + 12);

    compressed.push_back(0x78);
    compressed.push_back(0x9C);

    unsigned int adler = 1;
    unsigned int s1 = 1, s2 = 0;
    for (int y = 0; y < h; y++) {
        compressed.push_back(0);
        for (int x = 0; x < w; x++) {
            for (int c = 0; c < channels; c++) {
                unsigned char byte = raw[y * stride + x * channels + c];
                compressed.push_back(byte);
                s1 = (s1 + byte) % 65521;
                s2 = (s2 + s1) % 65521;
            }
        }
    }
    adler = (s2 << 16) | s1;

    compressed.push_back((adler >> 24) & 0xFF);
    compressed.push_back((adler >> 16) & 0xFF);
    compressed.push_back((adler >> 8) & 0xFF);
    compressed.push_back(adler & 0xFF);

    for (size_t i = 2; i < compressed.size() - 4; i += 65535) {
        size_t chunkSize = std::min<size_t>(65535, compressed.size() - 4 - i);
        out.push_back(0);
        out.push_back(0);
        out.push_back(0);
        out.push_back(static_cast<unsigned char>(chunkSize & 0xFF));
    }

    out.push_back(0);
    out.push_back(0);
    out.push_back(0);
    out.push_back(0);
    out.insert(out.end(), {'I', 'E', 'N', 'D'});
    uint32_t iendCrc = crc32(out.data() + idatStart + 4, 0);
    (void)iendCrc;

    return true;
}

CaptureResult captureScreen(CaptureSource source, ImageFormat fmt) {
    (void)fmt;
    CaptureResult result;
    result.success = false;
    result.channels = 3;

#ifdef _WIN32
    HDC hdcScreen = GetDC(nullptr);
    HDC hdcMem = CreateCompatibleDC(hdcScreen);
    int screenW = GetSystemMetrics(SM_CXSCREEN);
    int screenH = GetSystemMetrics(SM_CYSCREEN);

    if (source == CaptureSource::ActiveWindow) {
        HWND hwnd = GetForegroundWindow();
        if (hwnd) {
            RECT rect;
            GetClientRect(hwnd, &rect);
            screenW = rect.right - rect.left;
            screenH = rect.bottom - rect.top;
        }
    }

    HBITMAP hBitmap = CreateCompatibleBitmap(hdcScreen, screenW, screenH);
    SelectObject(hdcMem, hBitmap);
    BitBlt(hdcMem, 0, 0, screenW, screenH, hdcScreen, 0, 0, SRCCOPY);

    BITMAPINFOHEADER bi = {0};
    bi.biSize = sizeof(BITMAPINFOHEADER);
    bi.biWidth = screenW;
    bi.biHeight = -screenH;
    bi.biPlanes = 1;
    bi.biBitCount = 24;
    bi.biCompression = BI_RGB;

    std::vector<unsigned char> raw(screenW * screenH * 3);
    GetDIBits(hdcMem, hBitmap, 0, screenH, raw.data(), (BITMAPINFO*)&bi, DIB_RGB_COLORS);

    result.width = screenW;
    result.height = screenH;
    encodePng(raw, screenW, screenH, 3, result.data);
    result.success = true;

    DeleteObject(hBitmap);
    DeleteDC(hdcMem);
    ReleaseDC(nullptr, hdcScreen);
#else
    if (!display) {
        result.errorMsg = "X11 not initialized";
        return result;
    }

    Window root = RootWindow(display, screen);
    XWindowAttributes attr;
    XGetWindowAttributes(display, root, &attr);
    int w = attr.width;
    int h = attr.height;

    XImage* image = nullptr;
    if (hasShm) {
        XShmSegmentInfo shmInfo;
        image = XShmCreateImage(display, DefaultVisual(display, screen),
                                DefaultDepth(display, screen), ZPixmap,
                                nullptr, &shmInfo, w, h);
        if (image) {
            shmInfo.shmid = shmget(IPC_PRIVATE, image->bytes_per_line * image->height,
                                   IPC_CREAT | 0777);
            if (shmInfo.shmid >= 0) {
                shmInfo.shmaddr = image->data = (char*)shmat(shmInfo.shmid, nullptr, 0);
                shmInfo.readOnly = False;
                XShmAttach(display, &shmInfo);
                XShmGetImage(display, root, image, 0, 0, AllPlanes);
                XShmDetach(display, &shmInfo);
                shmdt(shmInfo.shmaddr);
                shmctl(shmInfo.shmid, IPC_RMID, nullptr);
            } else {
                XDestroyImage(image);
                image = nullptr;
                hasShm = false;
            }
        }
    }

    if (!image) {
        image = XGetImage(display, root, 0, 0, w, h, AllPlanes, ZPixmap);
    }

    if (image) {
        result.width = w;
        result.height = h;
        std::vector<unsigned char> raw(w * h * 3);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                unsigned long pixel = XGetPixel(image, x, y);
                raw[(y * w + x) * 3 + 0] = (pixel >> 16) & 0xFF;
                raw[(y * w + x) * 3 + 1] = (pixel >> 8) & 0xFF;
                raw[(y * w + x) * 3 + 2] = pixel & 0xFF;
            }
        }
        encodePng(raw, w, h, 3, result.data);
        result.success = true;
        XDestroyImage(image);
    }
#endif

    return result;
}

CaptureResult captureRegion(const CaptureRegion& region, ImageFormat fmt) {
    (void)fmt;
    CaptureResult result;
    result.success = false;
    result.channels = 3;

#ifdef _WIN32
    HDC hdcScreen = GetDC(nullptr);
    HDC hdcMem = CreateCompatibleDC(hdcScreen);
    HBITMAP hBitmap = CreateCompatibleBitmap(hdcScreen, region.width, region.height);
    SelectObject(hdcMem, hBitmap);
    BitBlt(hdcMem, 0, 0, region.width, region.height, hdcScreen,
           region.x, region.y, SRCCOPY);

    BITMAPINFOHEADER bi = {0};
    bi.biSize = sizeof(BITMAPINFOHEADER);
    bi.biWidth = region.width;
    bi.biHeight = -region.height;
    bi.biPlanes = 1;
    bi.biBitCount = 24;
    bi.biCompression = BI_RGB;

    std::vector<unsigned char> raw(region.width * region.height * 3);
    GetDIBits(hdcMem, hBitmap, 0, region.height, raw.data(), (BITMAPINFO*)&bi, DIB_RGB_COLORS);

    result.width = region.width;
    result.height = region.height;
    encodePng(raw, region.width, region.height, 3, result.data);
    result.success = true;

    DeleteObject(hBitmap);
    DeleteDC(hdcMem);
    ReleaseDC(nullptr, hdcScreen);
#else
    if (!display) {
        result.errorMsg = "X11 not initialized";
        return result;
    }
    Window root = RootWindow(display, screen);
    XImage* image = XGetImage(display, root, region.x, region.y,
                              region.width, region.height, AllPlanes, ZPixmap);
    if (image) {
        result.width = region.width;
        result.height = region.height;
        std::vector<unsigned char> raw(region.width * region.height * 3);
        for (int y = 0; y < region.height; y++) {
            for (int x = 0; x < region.width; x++) {
                unsigned long pixel = XGetPixel(image, x, y);
                raw[(y * region.width + x) * 3 + 0] = (pixel >> 16) & 0xFF;
                raw[(y * region.width + x) * 3 + 1] = (pixel >> 8) & 0xFF;
                raw[(y * region.width + x) * 3 + 2] = pixel & 0xFF;
            }
        }
        encodePng(raw, region.width, region.height, 3, result.data);
        result.success = true;
        XDestroyImage(image);
    }
#endif

    return result;
}

bool saveToFile(const CaptureResult& result, const std::string& path) {
    if (!result.success || result.data.empty()) return false;
    FILE* fp = fopen(path.c_str(), "wb");
    if (!fp) return false;
    size_t written = fwrite(result.data.data(), 1, result.data.size(), fp);
    fclose(fp);
    return written == result.data.size();
}

std::vector<std::string> listMonitors() {
    std::vector<std::string> monitors;
#ifdef _WIN32
    DISPLAY_DEVICE dd;
    dd.cb = sizeof(dd);
    for (DWORD i = 0; EnumDisplayDevices(nullptr, i, &dd, 0); i++) {
        if (dd.StateFlags & DISPLAY_DEVICE_ACTIVE) {
            monitors.push_back(dd.DeviceName);
        }
    }
#else
    if (display) {
        int nMonitors = ScreenCount(display);
        for (int i = 0; i < nMonitors; i++) {
            monitors.push_back("Screen " + std::to_string(i));
        }
    }
#endif
    return monitors;
}

}
}
