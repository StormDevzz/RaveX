#pragma once

#include "model_types.h"
#include "model_math.h"
#include <unordered_map>
#include <cstring>

namespace model {
namespace utils {

inline bool isFormatSupported(const std::string& ext) {
    return ext == "json" || ext == "bbmodel" ||
           ext == "gltf" || ext == "glb" ||
           ext == "obj" || ext == "pmx" ||
           ext == "pmd";
}

inline ModelFormat detectFormat(const uint8_t* data, size_t size) {
    if (size < 4) return ModelFormat::Unknown;
    if (data[0] == '{' || data[0] == '[') {
        std::string s(reinterpret_cast<const char*>(data), size < 256 ? size : 256);
        if (s.find("\"model\"") != std::string::npos ||
            s.find("\"textures\"") != std::string::npos)
            return ModelFormat::BBMODEL;
        if (s.find("\"parent\"") != std::string::npos ||
            s.find("\"elements\"") != std::string::npos)
            return ModelFormat::JSON;
        return ModelFormat::Unknown;
    }
    if (data[0] == 'g' && data[1] == 'l' && data[2] == 'T' && data[3] == 'F')
        return ModelFormat::GLTF;
    if (data[0] == 'v' || data[0] == 'o' || data[0] == 'f')
        return ModelFormat::OBJ;
    if (size > 4) {
        uint32_t magic;
        std::memcpy(&magic, data, 4);
        if (magic == 0x20504D58) return ModelFormat::PMX;
    }
    return ModelFormat::Unknown;
}

inline ModelFormat formatFromExt(const std::string& ext) {
    if (ext == "json") return ModelFormat::JSON;
    if (ext == "bbmodel") return ModelFormat::BBMODEL;
    if (ext == "gltf" || ext == "glb") return ModelFormat::GLTF;
    if (ext == "obj") return ModelFormat::OBJ;
    if (ext == "pmx" || ext == "pmd") return ModelFormat::PMX;
    return ModelFormat::Unknown;
}

inline const char* extForFormat(ModelFormat fmt) {
    switch (fmt) {
        case ModelFormat::JSON: return "json";
        case ModelFormat::BBMODEL: return "bbmodel";
        case ModelFormat::GLTF: return "gltf";
        case ModelFormat::OBJ: return "obj";
        case ModelFormat::PMX: return "pmx";
        default: return "bin";
    }
}

inline Mesh mergeMeshes(const std::vector<Mesh>& meshes) {
    Mesh out;
    uint32_t base = 0;
    for (auto& m : meshes) {
        out.vertices.insert(out.vertices.end(), m.vertices.begin(), m.vertices.end());
        for (auto& f : m.faces) {
            Face nf;
            for (int i = 0; i < 3; ++i) {
                nf.verts[i] = f.verts[i] + base;
                nf.uvs[i] = f.uvs[i] + base;
            }
            nf.material = f.material;
            out.faces.push_back(nf);
        }
        base += static_cast<uint32_t>(m.vertices.size());
    }
    return out;
}

} // namespace utils
} // namespace model
