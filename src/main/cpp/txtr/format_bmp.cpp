#include "txtr.hpp"
#include "include/txtr_utils.hpp"
#include "include/txtr_color.hpp"
#include <cstring>
#include <vector>

namespace txtr {

#pragma pack(push, 1)
struct BmpHeader {
    uint16_t bfType = 0x4D42;
    uint32_t bfSize = 0;
    uint16_t bfReserved1 = 0;
    uint16_t bfReserved2 = 0;
    uint32_t bfOffBits = 54;
};

struct BmpInfoHeader {
    uint32_t biSize = 40;
    int32_t  biWidth = 0;
    int32_t  biHeight = 0;
    uint16_t biPlanes = 1;
    uint16_t biBitCount = 32;
    uint32_t biCompression = 0;
    uint32_t biSizeImage = 0;
    int32_t  biXPelsPerMeter = 2835;
    int32_t  biYPelsPerMeter = 2835;
    uint32_t biClrUsed = 0;
    uint32_t biClrImportant = 0;
};
#pragma pack(pop)

TextureData loadBmp(const uint8_t* data, size_t size) {
    if (size < sizeof(BmpHeader) + sizeof(BmpInfoHeader))
        throw Error(ErrorCode::InvalidData, "BMP too small");

    BmpHeader hdr;
    std::memcpy(&hdr, data, sizeof(hdr));
    if (hdr.bfType != 0x4D42)
        throw Error(ErrorCode::InvalidData, "not a BMP");

    BmpInfoHeader info;
    std::memcpy(&info, data + sizeof(hdr), sizeof(info));

    int channels = 4;
    int width = std::abs(info.biWidth);
    int height = std::abs(info.biHeight);
    int rowBytes = utils::align4(width * (info.biBitCount / 8));
    bool topDown = info.biHeight < 0;

    TextureData tex;
    tex.width = width;
    tex.height = height;
    tex.channels = channels;
    tex.pixels.resize(static_cast<size_t>(width) * height * 4);

    uint32_t pixelOffset = hdr.bfOffBits;
    int srcBpp = info.biBitCount / 8;

    for (int y = 0; y < height; ++y) {
        int srcY = topDown ? y : (height - 1 - y);
        const uint8_t* src = data + pixelOffset + srcY * rowBytes;
        uint8_t* dst = tex.pixels.data() + y * tex.pitch();

        for (int x = 0; x < width; ++x) {
            if (srcBpp >= 3) {
                dst[x * 4 + 0] = src[x * srcBpp + 2];
                dst[x * 4 + 1] = src[x * srcBpp + 1];
                dst[x * 4 + 2] = src[x * srcBpp + 0];
            }
            dst[x * 4 + 3] = (srcBpp == 4) ? src[x * 4 + 3] : 0xFF;
        }
    }

    return tex;
}

ErrorCode saveBmp(const TextureData& tex, std::vector<uint8_t>& out, int) {
    int rowBytes = utils::align4(tex.width * 4);
    uint32_t pixelDataSize = rowBytes * tex.height;

    BmpHeader hdr;
    hdr.bfOffBits = sizeof(BmpHeader) + sizeof(BmpInfoHeader);
    hdr.bfSize = hdr.bfOffBits + pixelDataSize;

    BmpInfoHeader info;
    info.biWidth = tex.width;
    info.biHeight = tex.height;
    info.biBitCount = 32;
    info.biSizeImage = pixelDataSize;

    out.resize(hdr.bfSize);
    std::memcpy(out.data(), &hdr, sizeof(hdr));
    std::memcpy(out.data() + sizeof(hdr), &info, sizeof(info));

    for (int y = 0; y < tex.height; ++y) {
        int dstY = tex.height - 1 - y;
        const uint8_t* src = tex.pixels.data() + y * tex.pitch();
        uint8_t* dst = out.data() + hdr.bfOffBits + dstY * rowBytes;
        for (int x = 0; x < tex.width; ++x) {
            dst[x * 4 + 0] = src[x * 4 + 2];
            dst[x * 4 + 1] = src[x * 4 + 1];
            dst[x * 4 + 2] = src[x * 4 + 0];
            dst[x * 4 + 3] = src[x * 4 + 3];
        }
    }

    return ErrorCode::Success;
}

}
