#include "txtr.hpp"
#include "include/txtr_platform.hpp"
#include "include/txtr_utils.hpp"

#include <cstring>

namespace txtr {

using LoadFunc = TextureData (*)(const uint8_t*, size_t);
using SaveFunc = ErrorCode (*)(const TextureData&, std::vector<uint8_t>&, int);

struct FormatHandler {
    ImageFormat fmt;
    const char* magic;
    size_t magicLen;
    LoadFunc loadFn;
    SaveFunc saveFn;
};

extern TextureData loadPng(const uint8_t*, size_t);
extern TextureData loadJpeg(const uint8_t*, size_t);
extern TextureData loadBmp(const uint8_t*, size_t);
extern TextureData loadTga(const uint8_t*, size_t);
extern TextureData loadDds(const uint8_t*, size_t);

extern ErrorCode savePng(const TextureData&, std::vector<uint8_t>&, int);
extern ErrorCode saveJpeg(const TextureData&, std::vector<uint8_t>&, int);
extern ErrorCode saveBmp(const TextureData&, std::vector<uint8_t>&, int);
extern ErrorCode saveTga(const TextureData&, std::vector<uint8_t>&, int);
extern ErrorCode saveDds(const TextureData&, std::vector<uint8_t>&, int);

static FormatHandler handlers[] = {
    { ImageFormat::PNG,  "\x89PNG", 4, loadPng,  savePng },
    { ImageFormat::JPEG, "\xFF\xD8", 2, loadJpeg, saveJpeg },
    { ImageFormat::BMP,  "BM",      2, loadBmp,  saveBmp },
    { ImageFormat::TGA,  nullptr,   0, loadTga,  saveTga },
    { ImageFormat::DDS,  "DDS ",    4, loadDds,  saveDds },
};

static const FormatHandler* findHandler(ImageFormat fmt) {
    for (auto& h : handlers)
        if (h.fmt == fmt && h.loadFn)
            return &h;
    return nullptr;
}

static const FormatHandler* detectHandler(const uint8_t* data, size_t size) {
    for (auto& h : handlers) {
        if (h.magic && size >= h.magicLen &&
            std::memcmp(data, h.magic, h.magicLen) == 0)
            return &h;
    }
    return nullptr;
}

TextureData load(const std::string& path) {
    auto buf = platform::readFile(path);
    if (buf.empty()) throw Error(ErrorCode::FileNotFound, "could not read: " + path);
    return loadFromMemory(buf);
}

TextureData loadFromMemory(const uint8_t* data, size_t size) {
    if (!data || size == 0) throw Error(ErrorCode::InvalidArgument);
    auto handler = detectHandler(data, size);
    if (!handler) throw Error(ErrorCode::UnsupportedFormat);
    auto tex = handler->loadFn(data, size);
    if (!tex.valid()) throw Error(ErrorCode::DecodeError);
    return tex;
}

TextureData loadFromMemory(const std::vector<uint8_t>& data) {
    return loadFromMemory(data.data(), data.size());
}

ErrorCode save(const std::string& path, const TextureData& texture, ImageFormat format) {
    if (!texture.valid()) return ErrorCode::InvalidArgument;
    if (format == ImageFormat::Unknown) {
        auto ext = platform::getExtension(path);
        if (ext == "png") format = ImageFormat::PNG;
        else if (ext == "jpg" || ext == "jpeg") format = ImageFormat::JPEG;
        else if (ext == "bmp") format = ImageFormat::BMP;
        else if (ext == "tga") format = ImageFormat::TGA;
        else if (ext == "dds") format = ImageFormat::DDS;
        else return ErrorCode::UnsupportedFormat;
    }
    auto handler = findHandler(format);
    if (!handler || !handler->saveFn) return ErrorCode::UnsupportedFormat;
    std::vector<uint8_t> out;
    auto ec = handler->saveFn(texture, out, 90);
    if (ec != ErrorCode::Success) return ec;
    if (!platform::writeFile(path, out.data(), out.size()))
        return ErrorCode::FileWriteError;
    return ErrorCode::Success;
}

ErrorCode saveToMemory(std::vector<uint8_t>& out, const TextureData& texture, ImageFormat format, int quality) {
    if (!texture.valid()) return ErrorCode::InvalidArgument;
    auto handler = findHandler(format);
    if (!handler || !handler->saveFn) return ErrorCode::UnsupportedFormat;
    return handler->saveFn(texture, out, quality);
}

ImageInfo getInfo(const std::string& path) {
    ImageInfo info;
    auto buf = platform::readFile(path);
    if (buf.empty()) return info;
    info.fileSize = buf.size();
    info.format = utils::detectFormat(buf.data(), buf.size());
    try {
        auto tex = loadFromMemory(buf);
        info.width = tex.width;
        info.height = tex.height;
        info.channels = tex.channels;
        info.hasAlpha = (tex.channels == 4);
    } catch (...) {}
    return info;
}

ImageFormat detectFormat(const uint8_t* data, size_t size) {
    return utils::detectFormat(data, size);
}

bool isFormatSupported(ImageFormat fmt) {
    return findHandler(fmt) != nullptr;
}

} 
