#include "model.h"
#include "include/model_platform.h"
#include <iostream>
#include <cstring>
#include <string>

static void usage(const char* p) {
    std::cerr << "model - Model Converter\n"
              << "Usage:\n"
              << "  " << p << " info <file>\n"
              << "  " << p << " convert <input> <output>\n"
              << "  " << p << " optimize <input> <output>\n"
              << "  " << p << " ls <file>              - list meshes\n";
}

static int cmdInfo(const char* path) {
    try {
        auto m = model::load(path);
        std::cout << "File:    " << path << "\n"
                  << "Meshes:  " << m.meshes.size() << "\n"
                  << "Bones:   " << m.bones.size() << "\n"
                  << "Anims:   " << m.animations.size() << "\n"
                  << "Mats:    " << m.materials.size() << "\n"
                  << "Tex:     " << m.textures.size() << "\n";
        size_t verts = 0, faces = 0;
        for (auto& mesh : m.meshes) {
            verts += mesh.vertices.size();
            faces += mesh.faces.size();
        }
        std::cout << "Verts:   " << verts << "\n"
                  << "Faces:   " << faces << "\n";
    } catch (const std::exception& e) {
        std::cerr << "error: " << e.what() << "\n";
        return 1;
    }
    return 0;
}

static int cmdConvert(const char* in, const char* out) {
    try {
        auto m = model::load(in);
        model::save(out, m);
        std::cout << "saved to " << out << "\n";
    } catch (const std::exception& e) {
        std::cerr << "error: " << e.what() << "\n";
        return 1;
    }
    return 0;
}

static int cmdOptimize(const char* in, const char* out) {
    try {
        auto m = model::load(in);
        auto opt = model::optimize(m);
        model::save(out, opt);
        std::cout << "optimized -> " << out << "\n";
    } catch (const std::exception& e) {
        std::cerr << "error: " << e.what() << "\n";
        return 1;
    }
    return 0;
}

static int cmdList(const char* path) {
    try {
        auto m = model::load(path);
        for (size_t i = 0; i < m.meshes.size(); ++i) {
            auto& mesh = m.meshes[i];
            std::cout << "  [" << i << "] " << mesh.name
                      << " (" << mesh.vertices.size() << " verts, "
                      << mesh.faces.size() << " faces)\n";
        }
    } catch (const std::exception& e) {
        std::cerr << "error: " << e.what() << "\n";
        return 1;
    }
    return 0;
}

int main(int argc, char** argv) {
    if (argc < 3) { usage(argv[0]); return 1; }
    if (std::strcmp(argv[1], "info") == 0) return cmdInfo(argv[2]);
    if (std::strcmp(argv[1], "convert") == 0) return (argc < 4) ? (usage(argv[0]), 1) : cmdConvert(argv[2], argv[3]);
    if (std::strcmp(argv[1], "optimize") == 0) return (argc < 4) ? (usage(argv[0]), 1) : cmdOptimize(argv[2], argv[3]);
    if (std::strcmp(argv[1], "ls") == 0) return cmdList(argv[2]);
    usage(argv[0]);
    return 1;
}
