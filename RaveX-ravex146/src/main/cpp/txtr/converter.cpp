#include "txtr.hpp"
#include "include/txtr_color.hpp"
#include <cstring>

namespace txtr {

TextureData convert(const TextureData& input, const ConvertOptions& opts) {
    if (!input.valid()) throw Error(ErrorCode::InvalidArgument);

    TextureData result = input;

    if (opts.flipVertical) {
        std::vector<uint8_t> flipped(result.totalSize());
        size_t stride = result.pitch();
        for (int y = 0; y < result.height; ++y) {
            std::memcpy(flipped.data() + (result.height - 1 - y) * stride,
                        result.pixels.data() + y * stride, stride);
        }
        result.pixels = std::move(flipped);
    }

    if (opts.premultiplyAlpha && result.channels == 4) {
        color::premultiplyAlpha(result.pixels.data(),
            static_cast<size_t>(result.width) * result.height);
    }

    if (opts.outputFormat == ImageFormat::JPEG && result.channels == 4) {
        size_t count = static_cast<size_t>(result.width) * result.height;
        for (size_t i = 0; i < count; ++i)
            result.pixels[i * 4 + 3] = 0xFF;
    }

    return result;
}

}
