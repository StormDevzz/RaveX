#pragma once

#include "model_types.hpp"

namespace model {

struct ExportSettings {
    bool embedTextures = false;
    bool flipUV = false;
    int scale = 1;
    bool triangulate = true;
    bool generateNormals = true;
    bool exportAnimations = true;
    bool exportSkeleton = true;
};

struct ExportResult {
    std::vector<uint8_t> data;
    std::vector<std::string> warnings;
    bool success = false;
};

} 
