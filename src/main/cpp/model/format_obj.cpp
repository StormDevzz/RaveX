#include "model.h"

namespace model {

ModelData loadObj(const uint8_t*, size_t) {
    throw Error(ErrorCode::NotImplemented, "OBJ not implemented");
}

ExportResult saveObj(const ModelData&, const ExportSettings&) {
    throw Error(ErrorCode::NotImplemented, "OBJ not implemented");
}

} // namespace model
