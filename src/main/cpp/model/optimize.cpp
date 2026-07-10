#include "model.hpp"
#include "include/model_utils.hpp"
#include "include/model_math.hpp"

namespace model {

ModelData optimize(const ModelData& input) {
    ModelData out = input;

    for (auto& mesh : out.meshes) {
        if (!mesh.hasNormals && mesh.vertices.size() >= 3) {
            for (auto& f : mesh.faces) {
                auto& v0 = mesh.vertices[f.verts[0]];
                auto& v1 = mesh.vertices[f.verts[1]];
                auto& v2 = mesh.vertices[f.verts[2]];
                auto n = math::norm(math::cross(
                    math::sub(v1.pos, v0.pos),
                    math::sub(v2.pos, v0.pos)));
                v0.normal = n; v1.normal = n; v2.normal = n;
            }
            mesh.hasNormals = true;
        }
    }

    return out;
}

}
