#pragma once

#include "txtr_types.h"
#include "txtr_errors.h"

namespace txtr {

TextureData load(const std::string& path);
TextureData loadFromMemory(const uint8_t* data, size_t size);
TextureData loadFromMemory(const std::vector<uint8_t>& data);

ErrorCode save(const std::string& path, const TextureData& texture, ImageFormat format = ImageFormat::Unknown);
ErrorCode saveToMemory(std::vector<uint8_t>& out, const TextureData& texture, ImageFormat format, int quality = 90);

TextureData convert(const TextureData& input, const ConvertOptions& opts);
TextureData resize(const TextureData& input, const ResizeOptions& opts);
TextureData optimize(const TextureData& input, const OptimizeOptions& opts);

ImageInfo getInfo(const std::string& path);
ImageFormat detectFormat(const uint8_t* data, size_t size);

bool isFormatSupported(ImageFormat fmt);

} // namespace txtr
