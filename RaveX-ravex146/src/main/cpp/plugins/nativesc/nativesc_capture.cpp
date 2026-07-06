#include "include/nativesc.hpp"
#include "include/nativesc_types.hpp"
#include <cstring>

namespace ravex {
namespace nativesc {

CaptureResult captureToBuffer(CaptureSource source, PixelBuffer& buffer) {
    CaptureResult result = captureScreen(source);
    if (!result.success) return result;

    buffer.width = result.width;
    buffer.height = result.height;
    buffer.pixels.resize(result.width * result.height);

    for (int y = 0; y < result.height; y++) {
        for (int x = 0; x < result.width; x++) {
            int idx = (y * result.width + x) * result.channels;
            Color c;
            c.r = (result.channels > 0) ? result.data[idx] : 0;
            c.g = (result.channels > 1) ? result.data[idx + 1] : 0;
            c.b = (result.channels > 2) ? result.data[idx + 2] : 0;
            c.a = 255;
            buffer.setPixel(x, y, c);
        }
    }

    return result;
}

bool captureDifference(const PixelBuffer& a, const PixelBuffer& b,
                       std::vector<Color>& diff, int& diffCount) {
    if (a.width != b.width || a.height != b.height) return false;
    diffCount = 0;
    diff.resize(a.width * a.height);
    for (int y = 0; y < a.height; y++) {
        for (int x = 0; x < a.width; x++) {
            Color ca = a.getPixel(x, y);
            Color cb = b.getPixel(x, y);
            int dr = abs(ca.r - cb.r);
            int dg = abs(ca.g - cb.g);
            int db = abs(ca.b - cb.b);
            if (dr > 10 || dg > 10 || db > 10) {
                diff[y * a.width + x] = {255, 0, 0, 255};
                diffCount++;
            } else {
                diff[y * a.width + x] = ca;
            }
        }
    }
    return true;
}

}
}
