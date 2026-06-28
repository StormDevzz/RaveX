#pragma once

#include "txtr_types.h"
#include <cstring>
#include <cmath>
#include <cstdlib>

namespace txtr {
namespace utils {

template<typename T>
inline T clamp(T val, T min, T max) {
    return val < min ? min : (val > max ? max : val);
}

inline int align4(int v) { return (v + 3) & ~3; }

inline bool isPowerOfTwo(int v) { return v > 0 && (v & (v - 1)) == 0; }

inline int nextPowerOfTwo(int v) {
    int p = 1;
    while (p < v) p <<= 1;
    return p;
}

inline uint16_t readLe16(const uint8_t* data) {
    return static_cast<uint16_t>(data[0]) |
           (static_cast<uint16_t>(data[1]) << 8);
}

inline uint32_t readLe32(const uint8_t* data) {
    return static_cast<uint32_t>(data[0]) |
           (static_cast<uint32_t>(data[1]) << 8) |
           (static_cast<uint32_t>(data[2]) << 16) |
           (static_cast<uint32_t>(data[3]) << 24);
}

inline void writeLe16(uint8_t* dst, uint16_t v) {
    dst[0] = v & 0xFF;
    dst[1] = (v >> 8) & 0xFF;
}

inline void writeLe32(uint8_t* dst, uint32_t v) {
    dst[0] = v & 0xFF;
    dst[1] = (v >> 8) & 0xFF;
    dst[2] = (v >> 16) & 0xFF;
    dst[3] = (v >> 24) & 0xFF;
}

inline bool hasAlphaChannel(ImageFormat fmt) {
    return fmt == ImageFormat::PNG || fmt == ImageFormat::TGA;
}

inline bool isCompressedFormat(PixelFormat fmt) {
    return fmt == PixelFormat::DXT1 ||
           fmt == PixelFormat::DXT3 ||
           fmt == PixelFormat::DXT5;
}

inline ImageFormat detectFormat(const uint8_t* data, size_t size) {
    if (size < 4) return ImageFormat::Unknown;
    if (data[0] == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G')
        return ImageFormat::PNG;
    if (data[0] == 0xFF && data[1] == 0xD8)
        return ImageFormat::JPEG;
    if (data[0] == 'B' && data[1] == 'M')
        return ImageFormat::BMP;
    if ((data[0] == 0x44 && data[1] == 0x44 && data[2] == 0x53) ||
        (data[0] == 0x44 && data[1] == 0x44 && data[2] == 0x53 && data[3] == ' '))
        return ImageFormat::DDS;
    if ((data[0] == 0x00 || data[0] == 0x01) && data[1] == 0x00 &&
        size >= 18 && data[2] == 0x02 && data[3] == 0x00)
        return ImageFormat::TGA;
    return ImageFormat::Unknown;
}

} // namespace utils
} // namespace txtr
