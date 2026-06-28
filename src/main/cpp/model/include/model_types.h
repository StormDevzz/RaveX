#pragma once

#include <cstdint>
#include <string>
#include <vector>
#include <array>
#include <map>

namespace model {

struct Vec3 {
    float x = 0, y = 0, z = 0;
    Vec3() = default;
    Vec3(float x, float y, float z) : x(x), y(y), z(z) {}
};

struct Vec2 {
    float u = 0, v = 0;
    Vec2() = default;
    Vec2(float u, float v) : u(u), v(v) {}
};

struct Face {
    std::array<uint32_t, 3> verts = {};
    std::array<uint32_t, 3> uvs = {};
    uint32_t material = 0;
};

struct Vertex {
    Vec3 pos;
    Vec3 normal;
    Vec2 uv;
    std::array<uint8_t, 4> color = {255, 255, 255, 255};
};

struct Mesh {
    std::vector<Vertex> vertices;
    std::vector<Face> faces;
    std::vector<uint32_t> indices;
    std::string name;
    bool hasNormals = false;
    bool hasUVs = false;
};

struct Bone {
    std::string name;
    Vec3 pivot;
    Vec3 rotation;
    Vec3 position;
    int parent = -1;
};

struct AnimationKeyframe {
    float time;
    Vec3 value;
};

struct AnimationTrack {
    std::string boneName;
    std::vector<AnimationKeyframe> positionKeys;
    std::vector<AnimationKeyframe> rotationKeys;
    std::vector<AnimationKeyframe> scaleKeys;
};

struct Animation {
    std::string name;
    float length = 0;
    bool loop = false;
    std::vector<AnimationTrack> tracks;
};

struct Material {
    std::string name;
    std::string texturePath;
    std::array<float, 4> color = {1, 1, 1, 1};
    float roughness = 0.5f;
    float metalness = 0;
    bool transparent = false;
};

struct Texture {
    std::string name;
    std::string path;
    int width = 0;
    int height = 0;
};

struct ModelData {
    std::vector<Mesh> meshes;
    std::vector<Bone> bones;
    std::vector<Animation> animations;
    std::vector<Material> materials;
    std::vector<Texture> textures;
    bool valid() const { return !meshes.empty(); }
};

enum class ModelFormat : uint8_t {
    JSON,
    BBMODEL,
    GLTF,
    OBJ,
    PMX,
    Unknown
};

struct ConvertOptions {
    ModelFormat outputFormat = ModelFormat::GLTF;
    bool flattenHierarchy = false;
    bool mergeMeshes = false;
    bool deduplicateVerts = true;
};

struct ExportOptions {
    bool embedTextures = false;
    bool flipUV = false;
    int scale = 1;
};

} // namespace model
