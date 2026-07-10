#include "model.hpp"
#include "include/model_utils.hpp"

namespace model {

ModelData convert(const ModelData& input, const ConvertOptions& opts) {
    if (!input.valid()) throw Error(ErrorCode::InvalidArgument);
    ModelData out = input;

    if (opts.flattenHierarchy) {
        for (auto& mesh : out.meshes) {
            for (auto& v : mesh.vertices) {
                v.pos.x += 0;
            }
        }
        out.bones.clear();
    }

    if (opts.mergeMeshes && out.meshes.size() > 1) {
        auto merged = utils::mergeMeshes(out.meshes);
        out.meshes.clear();
        out.meshes.push_back(merged);
    }

    return out;
}

}
