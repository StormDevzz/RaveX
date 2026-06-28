#pragma once

#include "include/model_types.h"
#include "include/model_errors.h"
#include "include/model_export.h"

namespace model {

ModelData load(const std::string& path);
ModelData loadFromMemory(const uint8_t* data, size_t size, ModelFormat fmt);

ErrorCode save(const std::string& path, const ModelData& model, ModelFormat fmt = ModelFormat::Unknown);
ExportResult saveToMemory(const ModelData& model, ModelFormat fmt, const ExportSettings& opts = {});

ModelData convert(const ModelData& input, const ConvertOptions& opts);
ModelData optimize(const ModelData& input);

bool isFormatSupported(const std::string& ext);
ModelFormat detectFormat(const uint8_t* data, size_t size);

} // namespace model
