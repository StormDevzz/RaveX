#pragma once

#include <cstdint>
#include <cstddef>
#include <string>
#include <vector>

namespace txtr {

enum class PixelFormat : uint8_t {
    RGBA8,
    BGRA8,
    RGB8,
    BGR8,
    DXT1,
    DXT3,
    DXT5,
    A8,
    Unknown
};

enum class ImageFormat : uint8_t {
    PNG,
    JPEG,
    BMP,
    TGA,
    DDS,
    Unknown
};

struct TextureData {
    std::vector<uint8_t> pixels;
    int width = 0;
    int height = 0;
    int channels = 4;
    PixelFormat format = PixelFormat::RGBA8;

    bool valid() const { return width > 0 && height > 0 && !pixels.empty(); }
    size_t pitch() const { return static_cast<size_t>(width) * channels; }
    size_t totalSize() const { return pitch() * height; }
};

struct ConvertOptions {
    ImageFormat outputFormat = ImageFormat::PNG;
    int jpegQuality = 90;
    bool flipVertical = false;
    bool premultiplyAlpha = false;
};

struct ResizeOptions {
    int newWidth = 0;
    int newHeight = 0;
    bool keepAspectRatio = true;
    enum class Filter { Nearest, Bilinear, Bicubic, Lanczos } filter = Filter::Bilinear;
};

struct OptimizeOptions {
    bool stripAlpha = false;
    bool generateMipmaps = false;
    int maxDimension = 0;
    bool reduceColors = false;
    int maxColors = 256;
};

struct WatermarkOptions {
    int positionX = -1;
    int positionY = -1;
    enum class Placement { TopLeft, TopRight, BottomLeft, BottomRight, Center, Custom } placement = Placement::BottomRight;
    uint8_t opacity = 128;
    int margin = 8;
};

struct ImageInfo {
    int width = 0;
    int height = 0;
    int channels = 0;
    size_t fileSize = 0;
    ImageFormat format = ImageFormat::Unknown;
    bool hasAlpha = false;
};

} // namespace txtr
