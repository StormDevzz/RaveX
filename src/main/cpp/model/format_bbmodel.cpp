#include "model.h"
#include "include/model_json.h"
#include "include/model_math.h"

namespace model {

ModelData loadBbmodel(const uint8_t* data, size_t size) {
    std::string input(reinterpret_cast<const char*>(data), size);
    auto root = json::parse(input);
    ModelData out;

    if (root.get("textures").isArray()) {
        for (auto& t : root.get("textures").arr) {
            Texture tex;
            tex.name = t.get("name").s;
            tex.path = t.get("source").s;
            tex.width = t.get("width").asInt();
            tex.height = t.get("height").asInt();
            out.textures.push_back(tex);
        }
    }

    if (root.get("elements").isArray()) {
        Mesh mesh;
        mesh.name = "model";
        mesh.hasNormals = true;
        mesh.hasUVs = true;

        for (auto& e : root.get("elements").arr) {
            auto& from = e.get("from");
            auto& to = e.get("to");
            if (!from.isArray() || !to.isArray()) continue;

            float fx = from[0].asFloat(), fy = from[1].asFloat(), fz = from[2].asFloat();
            float tx = to[0].asFloat(),  ty = to[1].asFloat(),  tz = to[2].asFloat();

            uint32_t base = static_cast<uint32_t>(mesh.vertices.size());

            auto addQuad = [&](float x1, float y1, float z1, float x2, float y2, float z2,
                               float x3, float y3, float z3, float x4, float y4, float z4,
                               float nx, float ny, float nz,
                               float u1, float v1, float u2, float v2) {
                Vertex va, vb, vc, vd;
                va.pos = {x1,y1,z1}; vb.pos = {x2,y2,z2};
                vc.pos = {x3,y3,z3}; vd.pos = {x4,y4,z4};
                va.normal = vb.normal = vc.normal = vd.normal = {nx,ny,nz};
                va.uv = {u1,v1}; vb.uv = {u2,v1};
                vc.uv = {u2,v2}; vd.uv = {u1,v2};
                uint32_t i = static_cast<uint32_t>(mesh.vertices.size());
                mesh.vertices.push_back(va); mesh.vertices.push_back(vb);
                mesh.vertices.push_back(vc); mesh.vertices.push_back(vd);
                Face f1{{i,i+1,i+2},{}}, f2{{i,i+2,i+3},{}};
                mesh.faces.push_back(f1); mesh.faces.push_back(f2);
            };

            addQuad(fx,fy,fz, tx,fy,fz, tx,ty,fz, fx,ty,fz, 0,0,-1, 0,0,1,0);
            addQuad(fx,fy,tz, fx,ty,tz, tx,ty,tz, tx,fy,tz, 0,0,1, 0,0,1,1);
            addQuad(fx,fy,fz, fx,fy,tz, tx,fy,tz, tx,fy,fz, 0,-1,0, 0,0,1,0);
            addQuad(fx,ty,fz, tx,ty,fz, tx,ty,tz, fx,ty,tz, 0,1,0, 0,0,1,1);
            addQuad(fx,fy,fz, fx,ty,fz, fx,ty,tz, fx,fy,tz, -1,0,0, 0,0,1,0);
            addQuad(tx,fy,fz, tx,fy,tz, tx,ty,tz, tx,ty,fz, 1,0,0, 0,0,1,1);
        }

        out.meshes.push_back(mesh);
    }

    if (root.get("outliner").isArray()) {
        for (auto& item : root.get("outliner").arr) {
            if (item.get("name").isString()) {
                Bone bone;
                bone.name = item.get("name").s;
                if (item.get("origin").isArray()) {
                    auto& o = item.get("origin");
                    bone.pivot = {o[0].asFloat(), o[1].asFloat(), o[2].asFloat()};
                }
                out.bones.push_back(bone);
            }
        }
    }

    if (root.get("animations").isArray()) {
        for (auto& anim : root.get("animations").arr) {
            Animation a;
            a.name = anim.get("name").s;
            a.length = anim.get("length").asFloat();
            a.loop = anim.get("loop").b;
            out.animations.push_back(a);
        }
    }

    return out;
}

ExportResult saveBbmodel(const ModelData& m, const ExportSettings&) {
    std::string json = "{\"meta\":{\"format_version\":\"4.9\"},\"elements\":[";
    bool first = true;
    
    (void)m;
    json += "],\"textures\":[]}\n";
    ExportResult r;
    r.data.assign(json.begin(), json.end());
    r.success = true;
    return r;
}

} 
