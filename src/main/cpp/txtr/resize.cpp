#include "txtr.hpp"
#include "include/txtr_utils.hpp"
#include <cstring>
#include <algorithm>
#include <cmath>
#include <vector>

namespace txtr {

static uint8_t clampU8(float v) {
    return static_cast<uint8_t>(std::clamp(v, 0.0f, 255.0f));
}

static float getPixel(const uint8_t* src, int x, int y, int w, int h, int c, int channels) {
    x = std::clamp(x, 0, w - 1);
    y = std::clamp(y, 0, h - 1);
    return src[(y * w + x) * channels + c];
}

static void resizeNearest(const TextureData& input, TextureData& output) {
    float sx = static_cast<float>(input.width) / output.width;
    float sy = static_cast<float>(input.height) / output.height;
    for (int y = 0; y < output.height; ++y) {
        for (int x = 0; x < output.width; ++x) {
            int srcX = static_cast<int>(x * sx);
            int srcY = static_cast<int>(y * sy);
            std::memcpy(&output.pixels[(y * output.width + x) * output.channels],
                        &input.pixels[(srcY * input.width + srcX) * input.channels],
                        output.channels);
        }
    }
}

static void resizeBilinear(const TextureData& input, TextureData& output) {
    float sx = static_cast<float>(input.width) / output.width;
    float sy = static_cast<float>(input.height) / output.height;
    for (int y = 0; y < output.height; ++y) {
        for (int x = 0; x < output.width; ++x) {
            float gx = x * sx;
            float gy = y * sy;
            int ix = static_cast<int>(gx);
            int iy = static_cast<int>(gy);
            float fx = gx - ix;
            float fy = gy - iy;
            for (int c = 0; c < output.channels; ++c) {
                float v = (1 - fx) * (1 - fy) * getPixel(input.pixels.data(), ix, iy, input.width, input.height, c, input.channels) +
                          fx       * (1 - fy) * getPixel(input.pixels.data(), ix + 1, iy, input.width, input.height, c, input.channels) +
                          (1 - fx) * fy       * getPixel(input.pixels.data(), ix, iy + 1, input.width, input.height, c, input.channels) +
                          fx       * fy       * getPixel(input.pixels.data(), ix + 1, iy + 1, input.width, input.height, c, input.channels);
                output.pixels[(y * output.width + x) * output.channels + c] = clampU8(v);
            }
        }
    }
}

TextureData resize(const TextureData& input, const ResizeOptions& opts) {
    if (!input.valid()) throw Error(ErrorCode::InvalidArgument);

    int newW = opts.newWidth;
    int newH = opts.newHeight;

    if (newW <= 0 && newH <= 0)
        throw Error(ErrorCode::InvalidDimensions);

    if (opts.keepAspectRatio) {
        float ratio = static_cast<float>(input.width) / input.height;
        if (newW <= 0) {
            newW = static_cast<int>(newH * ratio);
        } else if (newH <= 0) {
            newH = static_cast<int>(newW / ratio);
        } else {
            float ar = static_cast<float>(newW) / newH;
            if (ar > ratio) newW = static_cast<int>(newH * ratio);
            else newH = static_cast<int>(newW / ratio);
        }
    }

    if (newW <= 0) newW = 1;
    if (newH <= 0) newH = 1;

    TextureData result;
    result.width = newW;
    result.height = newH;
    result.channels = input.channels;
    result.format = input.format;
    result.pixels.resize(static_cast<size_t>(newW) * newH * input.channels);

    switch (opts.filter) {
        case ResizeOptions::Filter::Nearest:
            resizeNearest(input, result);
            break;
        case ResizeOptions::Filter::Bilinear:
        default:
            resizeBilinear(input, result);
            break;
    }

    return result;
}

}
