#include "model.h"
#include "include/model_json.h"
#include <sstream>

namespace model {

static void skipSpace(const char*& p) {
    while (*p && (*p == ' ' || *p == '\t' || *p == '\n' || *p == '\r')) ++p;
}

static std::string parseStr(const char*& p) {
    skipSpace(p);
    std::string s;
    if (*p != '"') return s;
    ++p;
    while (*p && *p != '"') {
        if (*p == '\\') { ++p; if (*p) { s += *p++; } }
        else s += *p++;
    }
    if (*p) ++p;
    return s;
}

namespace json {

Value parse(const std::string& input) {
    const char* p = input.c_str();
    return detail::parseValue(p);
}

std::string serialize(const Value& v, int indent) {
    std::string r;
    std::string tab(indent, ' ');
    switch (v.type) {
        case Value::Null: r += "null"; break;
        case Value::Bool: r += v.b ? "true" : "false"; break;
        case Value::Number: r += std::to_string(v.n); break;
        case Value::String: r += "\"" + v.s + "\""; break;
        case Value::Array:
            r += "[\n";
            for (size_t i = 0; i < v.arr.size(); ++i) {
                r += tab + "  " + serialize(v.arr[i], indent + 2);
                if (i + 1 < v.arr.size()) r += ",";
                r += "\n";
            }
            r += tab + "]";
            break;
        case Value::Object:
            r += "{\n";
            {
                bool first = true;
                for (auto& [k, vv] : v.obj) {
                    if (!first) r += ",\n";
                    first = false;
                    r += tab + "  \"" + k + "\": " + serialize(vv, indent + 2);
                }
                r += "\n";
            }
            r += tab + "}";
            break;
    }
    return r;
}

namespace detail {

Value parseValue(const char*& p) {
    skipSpace(p);
    Value v;
    if (!*p) return v;
    if (*p == '"') { v.type = Value::String; v.s = parseStr(p); }
    else if (*p == '{') {
        v.type = Value::Object;
        ++p;
        skipSpace(p);
        if (*p == '}') { ++p; return v; }
        while (*p) {
            skipSpace(p);
            std::string key = parseStr(p);
            skipSpace(p);
            if (*p == ':') ++p;
            v.obj[key] = parseValue(p);
            skipSpace(p);
            if (*p == ',') ++p;
            else if (*p == '}') { ++p; break; }
        }
    }
    else if (*p == '[') {
        v.type = Value::Array;
        ++p;
        skipSpace(p);
        if (*p == ']') { ++p; return v; }
        while (*p) {
            v.arr.push_back(parseValue(p));
            skipSpace(p);
            if (*p == ',') ++p;
            else if (*p == ']') { ++p; break; }
        }
    }
    else if (*p == 't') { v.type = Value::Bool; v.b = true; p += 4; }
    else if (*p == 'f') { v.type = Value::Bool; v.b = false; p += 5; }
    else if (*p == 'n') { v.type = Value::Null; p += 4; }
    else {
        v.type = Value::Number;
        char* end = nullptr;
        v.n = std::strtod(p, &end);
        if (end) p = end;
    }
    return v;
}

} 
} 

ModelData loadJson(const uint8_t* data, size_t size) {
    std::string input(reinterpret_cast<const char*>(data), size);
    auto root = json::parse(input);
    if (!root.isObject()) throw Error(ErrorCode::JsonError, "root not object");

    if (!root.get("elements").isArray())
        throw Error(ErrorCode::JsonError, "no elements array");

    ModelData out;
    out.materials.resize(1);
    out.materials[0].name = "default";
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

        struct BVert { float x,y,z,nx,ny,nz,u,v; };
        BVert box[24] = {
            {fx,fy,fz,0,0,-1,0,0},{tx,fy,fz,0,0,-1,1,0},{tx,ty,fz,0,0,-1,1,1},{fx,ty,fz,0,0,-1,0,1},
            {fx,fy,tz,0,0,1,0,0},{tx,fy,tz,0,0,1,1,0},{tx,ty,tz,0,0,1,1,1},{fx,ty,tz,0,0,1,0,1},
            {fx,fy,fz,0,-1,0,0,0},{tx,fy,fz,0,-1,0,1,0},{tx,fy,tz,0,-1,0,1,1},{fx,fy,tz,0,-1,0,0,1},
            {fx,ty,fz,0,1,0,0,0},{tx,ty,fz,0,1,0,1,0},{tx,ty,tz,0,1,0,1,1},{fx,ty,tz,0,1,0,0,1},
            {fx,fy,fz,-1,0,0,0,0},{fx,ty,fz,-1,0,0,1,0},{fx,ty,tz,-1,0,0,1,1},{fx,fy,tz,-1,0,0,0,1},
            {tx,fy,fz,1,0,0,0,0},{tx,ty,fz,1,0,0,1,0},{tx,ty,tz,1,0,0,1,1},{tx,fy,tz,1,0,0,0,1},
        };
        static int bi[36] = {0,1,2,0,2,3,4,6,5,4,7,6,8,10,9,8,11,10,12,13,14,12,14,15,16,18,17,16,19,18,20,21,22,20,22,23};

        uint32_t base = static_cast<uint32_t>(mesh.vertices.size());
        for (int i = 0; i < 24; ++i) {
            Vertex v;
            v.pos = {box[i].x, box[i].y, box[i].z};
            v.normal = {box[i].nx, box[i].ny, box[i].nz};
            v.uv = {box[i].u, box[i].v};
            mesh.vertices.push_back(v);
        }
        for (int i = 0; i < 36; i += 3) {
            Face f;
            f.verts = {base + static_cast<uint32_t>(bi[i]), base + static_cast<uint32_t>(bi[i+1]), base + static_cast<uint32_t>(bi[i+2])};
            mesh.faces.push_back(f);
        }
    }

    out.meshes.push_back(mesh);
    return out;
}

ExportResult saveJson(const ModelData& m, const ExportSettings&) {
    std::ostringstream ss;
    ss << "{\"parent\":\"\",\"textures\":{},\"elements\":[";
    bool first = true;
    for (auto& mesh : m.meshes) {
        for (size_t fi = 0; fi < mesh.faces.size(); fi += 2) {
            if (fi + 1 >= mesh.faces.size()) break;
            auto& f = mesh.faces[fi];
            auto& v0 = mesh.vertices[f.verts[0]];
            if (!first) ss << ",";
            first = false;
            ss << "{\"from\":[" << (v0.pos.x-4) << "," << (v0.pos.y) << "," << (v0.pos.z-4)
               << "],\"to\":[" << (v0.pos.x+4) << "," << (v0.pos.y+8) << "," << (v0.pos.z+4)
               << "],\"faces\":{\"north\":{\"uv\":[0,0,16,16]}}}";
        }
    }
    ss << "]}";
    auto s = ss.str();
    ExportResult r;
    r.data.assign(s.begin(), s.end());
    r.success = true;
    return r;
}

} 
