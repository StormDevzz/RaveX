#pragma once
#include <cstdint>
#include <string>

namespace ravex {
namespace nativesc {

struct Color {
    uint8_t r;
    uint8_t g;
    uint8_t b;
    uint8_t a;
};

struct MonitorInfo {
    int id;
    std::string name;
    int x;
    int y;
    int width;
    int height;
    bool primary;
};

struct PixelBuffer {
    std::vector<Color> pixels;
    int width;
    int height;

    Color getPixel(int x, int y) const {
        if (x < 0 || x >= width || y < 0 || y >= height) return {0,0,0,0};
        return pixels[y * width + x];
    }

    void setPixel(int x, int y, Color c) {
        if (x < 0 || x >= width || y < 0 || y >= height) return;
        pixels[y * width + x] = c;
    }
};

}
}
