#include "model.h"

namespace model {

ModelData loadPmx(const uint8_t*, size_t) {
    throw Error(ErrorCode::NotImplemented, "PMX not implemented");
}

ExportResult savePmx(const ModelData&, const ExportSettings&) {
    throw Error(ErrorCode::NotImplemented, "PMX not implemented");
}

} // namespace model
