#include "include/nativesc.hpp"

#ifndef _WIN32
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/XShm.h>
#include <sys/ipc.h>
#include <sys/shm.h>

namespace ravex {
namespace nativesc {

extern Display* display;
extern int screen;
extern bool hasShm;

CaptureResult captureWindow(Window window) {
    CaptureResult result;
    result.success = false;
    result.channels = 3;

    if (!display) return result;

    XWindowAttributes attr;
    XGetWindowAttributes(display, window, &attr);
    int w = attr.width;
    int h = attr.height;

    XImage* image = XGetImage(display, window, 0, 0, w, h, AllPlanes, ZPixmap);
    if (!image) return result;

    std::vector<unsigned char> raw(w * h * 3);
    for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
            unsigned long pixel = XGetPixel(image, x, y);
            raw[(y * w + x) * 3 + 0] = (pixel >> 16) & 0xFF;
            raw[(y * w + x) * 3 + 1] = (pixel >> 8) & 0xFF;
            raw[(y * w + x) * 3 + 2] = pixel & 0xFF;
        }
    }

    result.width = w;
    result.height = h;
    encodePng(raw, w, h, 3, result.data);
    result.success = true;
    XDestroyImage(image);
    return result;
}

}
}
#endif
