#include "txtr.h"
#include "include/txtr_utils.h"
#include "include/txtr_color.h"
#include <cstring>
#include <vector>
#include <algorithm>

namespace txtr {

#pragma pack(push, 1)
struct DdsHeader {
    uint32_t magic = 0x20534444;
    uint32_t size = 124;
    uint32_t flags = 0x00021007;
    uint32_t height = 0;
    uint32_t width = 0;
    uint32_t pitchOrLinearSize = 0;
    uint32_t depth = 0;
    uint32_t mipMapCount = 0;
    uint32_t reserved1[11] = {};

    struct {
        uint32_t size = 32;
        uint32_t flags = 4;
        char     fourCC[4] = {};
        uint32_t rgbBitCount = 0;
        uint32_t rMask = 0;
        uint32_t gMask = 0;
        uint32_t bMask = 0;
        uint32_t aMask = 0;
    } pixelFormat;

    uint32_t caps = 0x00001000;
    uint32_t caps2 = 0;
    uint32_t caps3 = 0;
    uint32_t caps4 = 0;
    uint32_t reserved2 = 0;
};
#pragma pack(pop)

static void decompressDxt1(const uint8_t* src, uint8_t* dst, int w, int h) {
    for (int y = 0; y < h; y += 4) {
        for (int x = 0; x < w; x += 4) {
            uint16_t c0 = utils::readLe16(src);
            uint16_t c1 = utils::readLe16(src + 2);
            uint32_t bits = utils::readLe32(src + 4);
            src += 8;

            uint8_t colors[4][4];
            colors[0][0] = ((c0 >> 11) & 0x1F) << 3;
            colors[0][1] = ((c0 >> 5) & 0x3F) << 2;
            colors[0][2] = (c0 & 0x1F) << 3;
            colors[0][3] = 0xFF;
            colors[1][0] = ((c1 >> 11) & 0x1F) << 3;
            colors[1][1] = ((c1 >> 5) & 0x3F) << 2;
            colors[1][2] = (c1 & 0x1F) << 3;
            colors[1][3] = 0xFF;

            if (c0 > c1) {
                for (int i = 0; i < 3; ++i) {
                    colors[2][i] = (2 * colors[0][i] + colors[1][i] + 1) / 3;
                    colors[3][i] = (colors[0][i] + 2 * colors[1][i] + 1) / 3;
                }
                colors[2][3] = 0xFF;
                colors[3][3] = 0xFF;
            } else {
                for (int i = 0; i < 3; ++i) {
                    colors[2][i] = (colors[0][i] + colors[1][i] + 1) / 2;
                    colors[3][i] = 0;
                }
                colors[2][3] = 0xFF;
                colors[3][3] = 0;
            }

            for (int py = 0; py < 4; ++py) {
                for (int px = 0; px < 4; ++px) {
                    int idx = (bits >> (2 * (py * 4 + px))) & 3;
                    int dx = x + px;
                    int dy = y + py;
                    if (dx < w && dy < h) {
                        uint8_t* p = dst + (dy * w + dx) * 4;
                        std::memcpy(p, colors[idx], 4);
                    }
                }
            }
        }
    }
}

static void decompressDxt5(const uint8_t* src, uint8_t* dst, int w, int h) {
    for (int y = 0; y < h; y += 4) {
        for (int x = 0; x < w; x += 4) {
            uint8_t alphas[8];
            alphas[0] = src[0];
            alphas[1] = src[1];
            uint64_t alphaBits = 0;
            for (int i = 0; i < 6; ++i)
                alphaBits |= static_cast<uint64_t>(src[2 + i]) << (i * 8);
            src += 8;

            if (alphas[0] > alphas[1]) {
                for (int i = 2; i < 8; ++i)
                    alphas[i] = ((8 - i) * alphas[0] + (i - 1) * alphas[1] + 3) / 7;
            } else {
                for (int i = 2; i < 6; ++i)
                    alphas[i] = ((6 - i) * alphas[0] + (i - 1) * alphas[1] + 2) / 5;
                alphas[6] = 0;
                alphas[7] = 255;
            }

            uint16_t c0 = utils::readLe16(src);
            uint16_t c1 = utils::readLe16(src + 2);
            uint32_t bits = utils::readLe32(src + 4);
            src += 8;

            uint8_t cols[4][3];
            cols[0][0] = ((c0 >> 11) & 0x1F) << 3;
            cols[0][1] = ((c0 >> 5) & 0x3F) << 2;
            cols[0][2] = (c0 & 0x1F) << 3;
            cols[1][0] = ((c1 >> 11) & 0x1F) << 3;
            cols[1][1] = ((c1 >> 5) & 0x3F) << 2;
            cols[1][2] = (c1 & 0x1F) << 3;

            for (int i = 0; i < 3; ++i) {
                cols[2][i] = (2 * cols[0][i] + cols[1][i] + 1) / 3;
                cols[3][i] = (cols[0][i] + 2 * cols[1][i] + 1) / 3;
            }

            for (int py = 0; py < 4; ++py) {
                for (int px = 0; px < 4; ++px) {
                    int dx = x + px;
                    int dy = y + py;
                    if (dx < w && dy < h) {
                        uint8_t* p = dst + (dy * w + dx) * 4;
                        int ci = (bits >> (2 * (py * 4 + px))) & 3;
                        int ai = (alphaBits >> (3 * (py * 4 + px))) & 7;
                        std::memcpy(p, cols[ci], 3);
                        p[3] = alphas[ai];
                    }
                }
            }
        }
    }
}

TextureData loadDds(const uint8_t* data, size_t size) {
    if (size < sizeof(DdsHeader))
        throw Error(ErrorCode::InvalidData, "DDS too small");

    DdsHeader hdr;
    std::memcpy(&hdr, data, sizeof(hdr));

    int width = static_cast<int>(hdr.width);
    int height = static_cast<int>(hdr.height);

    TextureData tex;
    tex.width = width;
    tex.height = height;
    tex.channels = 4;
    tex.pixels.resize(static_cast<size_t>(width) * height * 4, 0xFF);

    const uint8_t* src = data + sizeof(hdr);

    if (std::memcmp(hdr.pixelFormat.fourCC, "DXT1", 4) == 0) {
        tex.format = PixelFormat::DXT1;
        decompressDxt1(src, tex.pixels.data(), width, height);
    } else if (std::memcmp(hdr.pixelFormat.fourCC, "DXT5", 4) == 0) {
        tex.format = PixelFormat::DXT5;
        decompressDxt5(src, tex.pixels.data(), width, height);
    } else if (std::memcmp(hdr.pixelFormat.fourCC, "DXT3", 4) == 0) {
        tex.format = PixelFormat::DXT3;
        decompressDxt5(src, tex.pixels.data(), width, height);
    } else if (hdr.pixelFormat.rgbBitCount == 32) {
        for (int y = 0; y < height; ++y) {
            uint8_t* dst = tex.pixels.data() + y * tex.pitch();
            const uint8_t* row = src + y * width * 4;
            for (int x = 0; x < width; ++x) {
                dst[x * 4 + 0] = row[x * 4 + 2];
                dst[x * 4 + 1] = row[x * 4 + 1];
                dst[x * 4 + 2] = row[x * 4 + 0];
                dst[x * 4 + 3] = row[x * 4 + 3];
            }
        }
    } else {
        throw Error(ErrorCode::UnsupportedFormat, "unsupported DXT variant");
    }

    return tex;
}

ErrorCode saveDds(const TextureData& tex, std::vector<uint8_t>& out, int) {
    DdsHeader hdr;
    hdr.width = tex.width;
    hdr.height = tex.height;
    hdr.pitchOrLinearSize = tex.width * tex.height * 4;
    std::memcpy(hdr.pixelFormat.fourCC, "DXT5", 4);
    hdr.caps = 0x00001000;

    size_t compressedSize = std::max(1, ((tex.width + 3) / 4)) *
                            std::max(1, ((tex.height + 3) / 4)) * 16;

    out.resize(sizeof(hdr) + compressedSize);
    std::memcpy(out.data(), &hdr, sizeof(hdr));

    uint8_t* dst = out.data() + sizeof(hdr);
    const uint8_t* src = tex.pixels.data();

    for (int y = 0; y < tex.height; y += 4) {
        for (int x = 0; x < tex.width; x += 4) {
            uint8_t block[16][4];
            int blockPixels = 0;
            uint8_t minAlpha = 255, maxAlpha = 0;

            for (int py = 0; py < 4; ++py) {
                for (int px = 0; px < 4; ++px) {
                    int bx = x + px;
                    int by = y + py;
                    if (bx < tex.width && by < tex.height) {
                        const uint8_t* p = src + (by * tex.width + bx) * 4;
                        std::memcpy(block[py * 4 + px], p, 4);
                        minAlpha = std::min(minAlpha, p[3]);
                        maxAlpha = std::max(maxAlpha, p[3]);
                        ++blockPixels;
                    }
                }
            }

            uint8_t alphaRef[2] = {maxAlpha, minAlpha};
            dst[0] = alphaRef[0];
            dst[1] = alphaRef[1];

            uint64_t alphaBits = 0;
            for (int i = 0; i < 16; ++i) {
                int ai;
                if (block[i][3] <= minAlpha) ai = 1;
                else if (block[i][3] >= maxAlpha) ai = 0;
                else ai = 0;
                alphaBits |= static_cast<uint64_t>(ai & 7) << (i * 3);
            }
            for (int i = 0; i < 6; ++i)
                dst[2 + i] = (alphaBits >> (i * 8)) & 0xFF;
            dst += 8;

            uint16_t c0 = 0, c1 = 0;
            uint32_t bits = 0;
            for (int i = 0; i < 16; ++i) {
                uint16_t r = block[i][0] >> 3;
                uint16_t g = block[i][1] >> 2;
                uint16_t b = block[i][2] >> 3;
                if (i == 0) { c0 = (r << 11) | (g << 5) | b; }
                else if (i == 1) { c1 = (r << 11) | (g << 5) | b; }
            }
            if (c0 < c1) std::swap(c0, c1);

            for (int i = 0; i < 16; ++i) {
                uint16_t r = block[i][0] >> 3;
                uint16_t g = block[i][1] >> 2;
                uint16_t b = block[i][2] >> 3;
                uint16_t pc = (r << 11) | (g << 5) | b;
                int idx = 0;
                int d0 = std::abs(static_cast<int>(pc) - static_cast<int>(c0));
                int d1 = std::abs(static_cast<int>(pc) - static_cast<int>(c1));
                idx = (d1 < d0) ? 1 : 0;
                bits |= static_cast<uint32_t>(idx & 3) << (i * 2);
            }

            utils::writeLe16(dst, c0);
            utils::writeLe16(dst + 2, c1);
            utils::writeLe32(dst + 4, bits);
            dst += 8;
        }
    }

    return ErrorCode::Success;
}

} // namespace txtr
