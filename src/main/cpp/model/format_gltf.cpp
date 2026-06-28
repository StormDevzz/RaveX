#include "model.h"

namespace model {

ModelData loadGltf(const uint8_t*, size_t) {
    throw Error(ErrorCode::NotImplemented, "GLTF not implemented");
}

ExportResult saveGltf(const ModelData&, const ExportSettings&) {
    throw Error(ErrorCode::NotImplemented, "GLTF not implemented");
}

} // namespace model
