#include "txtr.h"
#include "include/txtr_utils.h"
#include <cstring>
#include <vector>

namespace txtr {

#pragma pack(push, 1)
struct TgaHeader {
    uint8_t  idLength = 0;
    uint8_t  colorMapType = 0;
    uint8_t  imageType = 2;
    uint16_t colorMapStart = 0;
    uint16_t colorMapLength = 0;
    uint8_t  colorMapBits = 0;
    uint16_t xOrigin = 0;
    uint16_t yOrigin = 0;
    uint16_t width = 0;
    uint16_t height = 0;
    uint8_t  bpp = 32;
    uint8_t  descriptor = 0x28;
};
#pragma pack(pop)

TextureData loadTga(const uint8_t* data, size_t size) {
    if (size < sizeof(TgaHeader))
        throw Error(ErrorCode::InvalidData, "TGA too small");

    TgaHeader hdr;
    std::memcpy(&hdr, data, sizeof(hdr));

    if (hdr.imageType != 2 && hdr.imageType != 10)
        throw Error(ErrorCode::UnsupportedFormat, "unsupported TGA type");

    int channels = (hdr.bpp == 32) ? 4 : 3;
    int width = hdr.width;
    int height = hdr.height;
    bool topDown = (hdr.descriptor & 0x20) != 0;

    TextureData tex;
    tex.width = width;
    tex.height = height;
    tex.channels = 4;
    tex.pixels.resize(static_cast<size_t>(width) * height * 4, 0xFF);

    const uint8_t* src = data + sizeof(hdr) + hdr.idLength + hdr.colorMapLength * (hdr.colorMapBits / 8);

    if (hdr.imageType == 2) {
        for (int y = 0; y < height; ++y) {
            int srcY = topDown ? y : (height - 1 - y);
            uint8_t* dst = tex.pixels.data() + y * tex.pitch();

            for (int x = 0; x < width; ++x) {
                int si = (srcY * width + x) * channels;
                dst[x * 4 + 0] = src[si + 2];
                dst[x * 4 + 1] = src[si + 1];
                dst[x * 4 + 2] = src[si + 0];
                if (channels == 4) dst[x * 4 + 3] = src[si + 3];
                else dst[x * 4 + 3] = 0xFF;
            }
        }
    } else {
        int x = 0, y = 0;
        while (y < height) {
            uint8_t packet = *src++;
            bool rle = (packet & 0x80) != 0;
            int count = (packet & 0x7F) + 1;

            uint8_t pixel[4] = {};
            if (rle) {
                pixel[2] = src[0]; pixel[1] = src[1]; pixel[0] = src[2];
                pixel[3] = (channels == 4) ? src[3] : 0xFF;
                src += channels;
            }

            for (int i = 0; i < count; ++i) {
                int dstY = topDown ? y : (height - 1 - y);
                uint8_t* dst = tex.pixels.data() + dstY * tex.pitch() + x * 4;

                if (!rle) {
                    dst[0] = src[2]; dst[1] = src[1]; dst[2] = src[0];
                    dst[3] = (channels == 4) ? src[3] : 0xFF;
                    src += channels;
                } else {
                    std::memcpy(dst, pixel, 4);
                }

                if (++x >= width) { x = 0; ++y; }
                if (y >= height) break;
            }
        }
    }

    return tex;
}

ErrorCode saveTga(const TextureData& tex, std::vector<uint8_t>& out, int) {
    TgaHeader hdr;
    hdr.width = static_cast<uint16_t>(tex.width);
    hdr.height = static_cast<uint16_t>(tex.height);
    hdr.bpp = 32;
    hdr.descriptor = 0x28;
    hdr.imageType = 2;

    out.resize(sizeof(hdr) + tex.totalSize());
    std::memcpy(out.data(), &hdr, sizeof(hdr));

    uint8_t* dst = out.data() + sizeof(hdr);
    for (int y = 0; y < tex.height; ++y) {
        int srcY = tex.height - 1 - y;
        const uint8_t* src = tex.pixels.data() + srcY * tex.pitch();
        for (int x = 0; x < tex.width; ++x) {
            dst[(y * tex.width + x) * 4 + 0] = src[x * 4 + 2];
            dst[(y * tex.width + x) * 4 + 1] = src[x * 4 + 1];
            dst[(y * tex.width + x) * 4 + 2] = src[x * 4 + 0];
            dst[(y * tex.width + x) * 4 + 3] = src[x * 4 + 3];
        }
    }

    return ErrorCode::Success;
}

} // namespace txtr
