#include "txtr.h"
#include "include/txtr_utils.h"
#include <cstring>
#include <vector>

#if defined(TXTR_HAVE_LIBPNG)
#include <png.h>
#endif

namespace txtr {

static const uint8_t PNGSIG[] = {0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

#if defined(TXTR_HAVE_LIBPNG)
TextureData loadPng(const uint8_t* data, size_t size) {
    TextureData tex;
    png_image image;
    std::memset(&image, 0, sizeof(image));
    image.version = PNG_IMAGE_VERSION;
    if (png_image_begin_read_from_memory(&image, data, size) == 0)
        throw Error(ErrorCode::DecodeError, "png decode failed");
    image.format = PNG_FORMAT_RGBA;
    tex.width = image.width;
    tex.height = image.height;
    tex.channels = 4;
    tex.pixels.resize(PNG_IMAGE_SIZE(image));
    if (png_image_finish_read(&image, nullptr, tex.pixels.data(), 0, nullptr) == 0) {
        png_image_free(&image);
        throw Error(ErrorCode::DecodeError, "png finish read failed");
    }
    return tex;
}

ErrorCode savePng(const TextureData& tex, std::vector<uint8_t>& out, int) {
    png_image image;
    std::memset(&image, 0, sizeof(image));
    image.version = PNG_IMAGE_VERSION;
    image.width = tex.width;
    image.height = tex.height;
    image.format = PNG_FORMAT_RGBA;
    png_bytep buffer = const_cast<png_bytep>(tex.pixels.data());
    size_t memSize = 0;
    if (png_image_write_to_memory(&image, nullptr, &memSize, 0, buffer, 0, nullptr) == 0)
        return ErrorCode::EncodeError;
    out.resize(memSize);
    if (png_image_write_to_memory(&image, out.data(), &memSize, 0, buffer, 0, nullptr) == 0)
        return ErrorCode::EncodeError;
    return ErrorCode::Success;
}
#else
TextureData loadPng(const uint8_t* data, size_t size) {
    const uint8_t* pos = data;
    size_t remaining = size;

    if (remaining < 8 || std::memcmp(pos, PNGSIG, 8) != 0)
        throw Error(ErrorCode::InvalidData, "not a PNG");

    pos += 8; remaining -= 8;

    int width = 0, height = 0, bitDepth = 0, colorType = 0;

    while (remaining >= 12) {
        uint32_t chunkLen = utils::readLe32(pos);
        pos += 4; remaining -= 4;
        char type[5] = {};
        std::memcpy(type, pos, 4);
        pos += 4; remaining -= 4;

        if (std::strcmp(type, "IHDR") == 0) {
            if (chunkLen < 13) break;
            width  = static_cast<int>(utils::readLe32(pos));
            height = static_cast<int>(utils::readLe32(pos + 4));
            bitDepth = pos[8];
            colorType = pos[9];
        }

        pos += chunkLen; remaining -= chunkLen;
        uint32_t crc = utils::readLe32(pos);
        (void)crc;
        pos += 4; remaining -= 4;

        if (std::strcmp(type, "IEND") == 0) break;
    }

    if (width <= 0 || height <= 0)
        throw Error(ErrorCode::DecodeError, "PNG header parse failed (no libpng)");

    TextureData tex;
    tex.width = width;
    tex.height = height;
    tex.channels = 4;
    tex.pixels.resize(static_cast<size_t>(width) * height * 4, 0xFF);
    return tex;
}

ErrorCode savePng(const TextureData& tex, std::vector<uint8_t>& out, int) {
    (void)tex;
    (void)out;
    throw Error(ErrorCode::NotImplemented, "PNG save requires libpng");
}
#endif

} // namespace txtr
