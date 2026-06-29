#include "model.h"
#include "include/model_platform.h"
#include "include/model_utils.h"

namespace model {

using LoadFn = ModelData (*)(const uint8_t*, size_t);
using SaveFn = ExportResult (*)(const ModelData&, const ExportSettings&);

struct FormatHandler {
    ModelFormat fmt;
    const char* name;
    const char* magic;
    size_t magicLen;
    LoadFn loadFn;
    SaveFn saveFn;
};

extern ModelData loadJson(const uint8_t*, size_t);
extern ModelData loadBbmodel(const uint8_t*, size_t);
extern ModelData loadGltf(const uint8_t*, size_t);
extern ModelData loadObj(const uint8_t*, size_t);
extern ModelData loadPmx(const uint8_t*, size_t);

extern ExportResult saveJson(const ModelData&, const ExportSettings&);
extern ExportResult saveBbmodel(const ModelData&, const ExportSettings&);
extern ExportResult saveGltf(const ModelData&, const ExportSettings&);
extern ExportResult saveObj(const ModelData&, const ExportSettings&);
extern ExportResult savePmx(const ModelData&, const ExportSettings&);

static FormatHandler handlers[] = {
    {ModelFormat::JSON, "json",    nullptr, 0, loadJson,    saveJson},
    {ModelFormat::BBMODEL, "bbmodel", nullptr, 0, loadBbmodel, saveBbmodel},
    {ModelFormat::GLTF, "gltf",   "glTF", 4, loadGltf,   saveGltf},
    {ModelFormat::OBJ,  "obj",    nullptr, 0, loadObj,    saveObj},
    {ModelFormat::PMX,  "pmx",    nullptr, 0, loadPmx,    savePmx},
};

static const FormatHandler* findHandler(ModelFormat fmt) {
    for (auto& h : handlers)
        if (h.fmt == fmt) return &h;
    return nullptr;
}

ModelData load(const std::string& path) {
    auto buf = platform::readFile(path);
    if (buf.empty()) throw Error(ErrorCode::FileNotFound, path);
    auto ext = platform::getExt(path);
    auto fmt = utils::formatFromExt(ext);
    if (fmt == ModelFormat::Unknown)
        fmt = utils::detectFormat(buf.data(), buf.size());
    return loadFromMemory(buf.data(), buf.size(), fmt);
}

ModelData loadFromMemory(const uint8_t* data, size_t size, ModelFormat fmt) {
    if (!data || !size) throw Error(ErrorCode::InvalidArgument);
    if (fmt == ModelFormat::Unknown) fmt = utils::detectFormat(data, size);
    auto h = findHandler(fmt);
    if (!h || !h->loadFn) throw Error(ErrorCode::UnsupportedFormat);
    auto m = h->loadFn(data, size);
    if (!m.valid()) throw Error(ErrorCode::DecodeError);
    return m;
}

ErrorCode save(const std::string& path, const ModelData& model, ModelFormat fmt) {
    if (!model.valid()) return ErrorCode::InvalidArgument;
    if (fmt == ModelFormat::Unknown) {
        auto ext = platform::getExt(path);
        fmt = utils::formatFromExt(ext);
    }
    auto h = findHandler(fmt);
    if (!h || !h->saveFn) return ErrorCode::UnsupportedFormat;
    ExportSettings opts;
    auto res = h->saveFn(model, opts);
    if (!res.success) return ErrorCode::EncodeError;
    if (!platform::writeFile(path, res.data.data(), res.data.size()))
        return ErrorCode::FileWriteError;
    return ErrorCode::Success;
}

ExportResult saveToMemory(const ModelData& model, ModelFormat fmt, const ExportSettings& opts) {
    auto h = findHandler(fmt);
    if (!h || !h->saveFn) {
        ExportResult r; r.success = false; return r;
    }
    return h->saveFn(model, opts);
}

ModelData convert(const ModelData& input, const ConvertOptions& opts) {
    if (!input.valid()) throw Error(ErrorCode::InvalidArgument);
    ModelData out = input;

    if (opts.flattenHierarchy) {
        math::Mat4 global;
        for (auto& mesh : out.meshes) {
            for (auto& v : mesh.vertices)
                v.pos = global.transform(v.pos);
        }
        out.bones.clear();
    }

    if (opts.mergeMeshes && out.meshes.size() > 1) {
        auto merged = utils::mergeMeshes(out.meshes);
        out.meshes.clear();
        out.meshes.push_back(merged);
    }

    if (opts.deduplicateVerts) {
        
    }

    return out;
}

ModelData optimize(const ModelData& input) {
    ModelData out = input;
    for (auto& mesh : out.meshes) {
        if (!mesh.hasNormals && mesh.vertices.size() >= 3) {
            for (size_t i = 0; i < mesh.faces.size(); ++i) {
                auto& f = mesh.faces[i];
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

bool isFormatSupported(const std::string& ext) {
    return utils::isFormatSupported(ext);
}

ModelFormat detectFormat(const uint8_t* data, size_t size) {
    return utils::detectFormat(data, size);
}

} 
