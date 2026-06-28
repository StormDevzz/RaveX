#include "txtr.h"
#include "include/txtr_color.h"
#include "include/txtr_utils.h"
#include <cstring>
#include <climits>
#include <vector>
#include <map>
#include <algorithm>

namespace txtr {

TextureData optimize(const TextureData& input, const OptimizeOptions& opts) {
    if (!input.valid()) throw Error(ErrorCode::InvalidArgument);

    TextureData result = input;

    if (opts.maxDimension > 0 &&
        (result.width > opts.maxDimension || result.height > opts.maxDimension)) {
        ResizeOptions rOpts;
        rOpts.newWidth = opts.maxDimension;
        rOpts.newHeight = opts.maxDimension;
        rOpts.keepAspectRatio = true;
        rOpts.filter = ResizeOptions::Filter::Lanczos;
        result = resize(result, rOpts);
    }

    if (opts.stripAlpha && result.channels == 4) {
        size_t count = static_cast<size_t>(result.width) * result.height;
        for (size_t i = 0; i < count; ++i)
            result.pixels[i * 4 + 3] = 0xFF;
    }

    if (opts.generateMipmaps) {
        TextureData mipmapped;
        mipmapped.width = result.width;
        mipmapped.height = result.height;
        mipmapped.channels = result.channels;
        mipmapped.format = result.format;
        mipmapped.pixels = result.pixels;

        int mw = result.width;
        int mh = result.height;
        while (mw > 1 || mh > 1) {
            mw = std::max(1, mw / 2);
            mh = std::max(1, mh / 2);
            ResizeOptions rOpts;
            rOpts.newWidth = mw;
            rOpts.newHeight = mh;
            rOpts.keepAspectRatio = false;
            rOpts.filter = ResizeOptions::Filter::Bilinear;
            mipmapped = resize(result, rOpts);
        }
        result = std::move(mipmapped);
    }

    if (opts.reduceColors && result.channels == 4) {
        int maxColors = std::max(2, opts.maxColors);
        std::map<uint32_t, size_t> colorCount;
        size_t count = static_cast<size_t>(result.width) * result.height;

        for (size_t i = 0; i < count; ++i) {
            uint32_t c = color::packRgba(
                result.pixels[i * 4 + 0],
                result.pixels[i * 4 + 1],
                result.pixels[i * 4 + 2],
                result.pixels[i * 4 + 3]
            );
            auto it = colorCount.find(c);
            if (it != colorCount.end()) it->second++;
            else colorCount[c] = 1;
        }

        if (static_cast<int>(colorCount.size()) > maxColors) {
            std::vector<std::pair<uint32_t, size_t>> sorted(colorCount.begin(), colorCount.end());
            std::sort(sorted.begin(), sorted.end(),
                [](const auto& a, const auto& b) { return a.second > b.second; });

            sorted.resize(maxColors);
            std::map<uint32_t, uint32_t> palette;
            for (const auto& [color, _] : sorted)
                palette[color] = color;

            for (size_t i = 0; i < count; ++i) {
                uint32_t c = color::packRgba(
                    result.pixels[i * 4 + 0],
                    result.pixels[i * 4 + 1],
                    result.pixels[i * 4 + 2],
                    result.pixels[i * 4 + 3]
                );
                if (palette.find(c) == palette.end()) {
                    uint32_t nearest = sorted[0].first;
                    int bestDist = INT_MAX;
                    for (const auto& [pc, _] : palette) {
                        uint8_t pr, pg, pb, pa, cr, cg, cb, ca;
                        color::unpackRgba(pc, pr, pg, pb, pa);
                        color::unpackRgba(c, cr, cg, cb, ca);
                        int d = std::abs(static_cast<int>(pr) - static_cast<int>(cr)) +
                                std::abs(static_cast<int>(pg) - static_cast<int>(cg)) +
                                std::abs(static_cast<int>(pb) - static_cast<int>(cb)) +
                                std::abs(static_cast<int>(pa) - static_cast<int>(ca));
                        if (d < bestDist) { bestDist = d; nearest = pc; }
                    }
                    c = nearest;
                }
                color::unpackRgba(c,
                    result.pixels[i * 4 + 0],
                    result.pixels[i * 4 + 1],
                    result.pixels[i * 4 + 2],
                    result.pixels[i * 4 + 3]);
            }
        }
    }

    return result;
}

} // namespace txtr
