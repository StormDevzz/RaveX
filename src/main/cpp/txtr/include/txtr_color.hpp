#pragma once

#include "txtr_types.hpp"
#include <algorithm>
#include <cmath>

namespace txtr {
namespace color {

inline void rgbaToBgra(uint8_t* data, size_t pixelCount) {
    for (size_t i = 0; i < pixelCount; ++i) {
        std::swap(data[i * 4 + 0], data[i * 4 + 2]);
    }
}

inline void bgraToRgba(uint8_t* data, size_t pixelCount) {
    rgbaToBgra(data, pixelCount);
}

inline uint8_t toGray(const uint8_t* rgba) {
    return static_cast<uint8_t>(
        0.299f * rgba[0] + 0.587f * rgba[1] + 0.114f * rgba[2]
    );
}

inline void toGrayscale(uint8_t* data, size_t pixelCount, int channels) {
    for (size_t i = 0; i < pixelCount; ++i) {
        uint8_t gray = toGray(&data[i * channels]);
        data[i * channels + 0] = gray;
        data[i * channels + 1] = gray;
        data[i * channels + 2] = gray;
    }
}

inline void premultiplyAlpha(uint8_t* data, size_t pixelCount) {
    for (size_t i = 0; i < pixelCount; ++i) {
        uint8_t* p = &data[i * 4];
        float a = p[3] / 255.0f;
        p[0] = static_cast<uint8_t>(p[0] * a);
        p[1] = static_cast<uint8_t>(p[1] * a);
        p[2] = static_cast<uint8_t>(p[2] * a);
    }
}

inline void unpremultiplyAlpha(uint8_t* data, size_t pixelCount) {
    for (size_t i = 0; i < pixelCount; ++i) {
        uint8_t* p = &data[i * 4];
        float a = p[3] / 255.0f;
        if (a > 0.001f) {
            p[0] = static_cast<uint8_t>(std::min(255.0f, p[0] / a));
            p[1] = static_cast<uint8_t>(std::min(255.0f, p[1] / a));
            p[2] = static_cast<uint8_t>(std::min(255.0f, p[2] / a));
        }
    }
}

inline uint32_t packRgba(uint8_t r, uint8_t g, uint8_t b, uint8_t a) {
    return (static_cast<uint32_t>(r) << 24) |
           (static_cast<uint32_t>(g) << 16) |
           (static_cast<uint32_t>(b) << 8)  |
           a;
}

inline void unpackRgba(uint32_t packed, uint8_t& r, uint8_t& g, uint8_t& b, uint8_t& a) {
    r = (packed >> 24) & 0xFF;
    g = (packed >> 16) & 0xFF;
    b = (packed >> 8) & 0xFF;
    a = packed & 0xFF;
}

} 
} 
