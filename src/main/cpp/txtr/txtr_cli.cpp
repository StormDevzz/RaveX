#include "txtr.h"
#include "include/txtr_platform.h"
#include <iostream>
#include <fstream>
#include <cstring>
#include <string>
#include <vector>
#include <dirent.h>
#include <sys/stat.h>
#include <cstdlib>

static const char* formatExt(txtr::ImageFormat fmt) {
    switch (fmt) {
        case txtr::ImageFormat::PNG:  return "png";
        case txtr::ImageFormat::JPEG: return "jpg";
        case txtr::ImageFormat::BMP:  return "bmp";
        case txtr::ImageFormat::TGA:  return "tga";
        case txtr::ImageFormat::DDS:  return "dds";
        default: return "bin";
    }
}

static txtr::ImageFormat parseFormat(const char* s) {
    if (std::strcmp(s, "png") == 0) return txtr::ImageFormat::PNG;
    if (std::strcmp(s, "jpg") == 0 || std::strcmp(s, "jpeg") == 0) return txtr::ImageFormat::JPEG;
    if (std::strcmp(s, "bmp") == 0) return txtr::ImageFormat::BMP;
    if (std::strcmp(s, "tga") == 0) return txtr::ImageFormat::TGA;
    if (std::strcmp(s, "dds") == 0) return txtr::ImageFormat::DDS;
    return txtr::ImageFormat::Unknown;
}

static bool isImageExt(const std::string& ext) {
    return ext == "png" || ext == "jpg" || ext == "jpeg" ||
           ext == "bmp" || ext == "tga" || ext == "dds";
}

static std::string replaceExt(const std::string& path, const std::string& newExt) {
    auto pos = path.rfind('.');
    if (pos == std::string::npos) return path + "." + newExt;
    return path.substr(0, pos + 1) + newExt;
}

static std::vector<std::string> listDir(const std::string& dir, bool recursive) {
    std::vector<std::string> files;
    DIR* d = opendir(dir.c_str());
    if (!d) return files;
    struct dirent* entry;
    while ((entry = readdir(d)) != nullptr) {
        std::string name = entry->d_name;
        if (name == "." || name == "..") continue;
        std::string full = dir + "/" + name;
        struct stat st;
        if (stat(full.c_str(), &st) != 0) continue;
        if (S_ISDIR(st.st_mode)) {
            if (recursive) {
                auto sub = listDir(full, true);
                files.insert(files.end(), sub.begin(), sub.end());
            }
        } else if (S_ISREG(st.st_mode)) {
            auto ext = txtr::platform::getExtension(name);
            if (isImageExt(ext))
                files.push_back(full);
        }
    }
    closedir(d);
    return files;
}

static bool ensureDir(const std::string& path) {
    if (path.empty()) return true;
    struct stat st;
    if (stat(path.c_str(), &st) == 0 && S_ISDIR(st.st_mode))
        return true;
    return mkdir(path.c_str(), 0755) == 0;
}

// --- interactive ----------------------------------------------------------

static void interactive() {
    std::cout << "\ntxtr - Texture Tool v1.0  (type 'help' for commands)\n\n";

    while (true) {
        std::cout << "txtr> ";
        std::cout.flush();

        std::string line;
        if (!std::getline(std::cin, line)) break;

        if (line.empty()) continue;

        if (line == "exit" || line == "quit") break;

        if (line == "help") {
            std::cout << "Commands:\n"
                      << "  info <file>              - show image info\n"
                      << "  convert <in> <out>       - convert image\n"
                      << "  resize <in> <out> W H    - resize\n"
                      << "  optimize <in> <out>      - optimize\n"
                      << "  batch <dir> [-f fmt]      - batch convert directory\n"
                      << "  batch <dir> <outdir> [-f fmt]  - batch with output dir\n"
                      << "  exit                     - quit\n";
            continue;
        }

        auto args = std::vector<std::string>();
        size_t pos = 0;
        while (pos < line.size()) {
            while (pos < line.size() && line[pos] == ' ') ++pos;
            if (pos >= line.size()) break;
            size_t end = line.find(' ', pos);
            if (end == std::string::npos) end = line.size();
            args.push_back(line.substr(pos, end - pos));
            pos = end;
        }

        if (args.empty()) continue;
        auto& cmd = args[0];

        try {
            if (cmd == "info" && args.size() >= 2) {
                auto info = txtr::getInfo(args[1]);
                std::cout << "  File:     " << args[1] << "\n"
                          << "  Size:     " << info.width << "x" << info.height << "\n"
                          << "  Channels: " << info.channels << "\n"
                          << "  Bytes:    " << info.fileSize << "\n"
                          << "  Alpha:    " << (info.hasAlpha ? "yes" : "no") << "\n";
            }
            else if (cmd == "convert" && args.size() >= 3) {
                auto tex = txtr::load(args[1]);
                txtr::save(args[2], tex);
                std::cout << "  -> " << args[2] << "\n";
            }
            else if (cmd == "resize" && args.size() >= 4) {
                txtr::ResizeOptions opts;
                opts.newWidth = std::stoi(args[2]);
                opts.newHeight = std::stoi(args[3]);
                std::string out = (args.size() >= 5) ? args[4] : args[1];
                auto tex = txtr::load(args[1]);
                auto resized = txtr::resize(tex, opts);
                txtr::save(out, resized);
                std::cout << "  " << tex.width << "x" << tex.height
                          << " -> " << resized.width << "x" << resized.height << "\n";
            }
            else if (cmd == "optimize" && args.size() >= 2) {
                txtr::OptimizeOptions opts;
                opts.generateMipmaps = true;
                opts.maxDimension = 1024;
                std::string out = (args.size() >= 3) ? args[2] : args[1];
                auto tex = txtr::load(args[1]);
                auto optimized = txtr::optimize(tex, opts);
                txtr::save(out, optimized);
                std::cout << "  " << tex.totalSize() << " -> " << optimized.totalSize() << " bytes\n";
            }
            else if (cmd == "batch") {
                txtr::ImageFormat outFmt = txtr::ImageFormat::DDS;
                std::string outDir;
                std::string inDir;

                if (args.size() >= 2) inDir = args[1];
                size_t argIdx = 2;
                if (argIdx < args.size() && args[argIdx][0] != '-') {
                    outDir = args[argIdx++];
                }
                while (argIdx < args.size()) {
                    if (args[argIdx] == "-f" && argIdx + 1 < args.size()) {
                        outFmt = parseFormat(args[argIdx + 1].c_str());
                        argIdx += 2;
                    } else break;
                }

                bool useExtFormat = (outFmt != txtr::ImageFormat::Unknown);

                auto files = listDir(inDir, true);
                if (files.empty()) {
                    std::cout << "  no images found in " << inDir << "\n";
                    continue;
                }

                if (outDir.empty()) {
                    outDir = inDir + "/converted";
                }
                ensureDir(outDir);

                int ok = 0, fail = 0;
                for (auto& f : files) {
                    std::string ext = txtr::platform::getExtension(f);
                    txtr::ImageFormat fmt = useExtFormat ? outFmt : parseFormat(ext.c_str());
                    if (fmt == txtr::ImageFormat::Unknown) { ++fail; continue; }

                    auto baseName = f.substr(f.rfind('/') + 1);
                    std::string outFile = outDir + "/" + replaceExt(baseName, formatExt(fmt));

                    try {
                        auto tex = txtr::load(f);
                        txtr::save(outFile, tex, fmt);
                        std::cout << "  [+] " << baseName << " -> " << outFile << "\n";
                        ++ok;
                    } catch (const std::exception& e) {
                        std::cout << "  [X] " << baseName << ": " << e.what() << "\n";
                        ++fail;
                    }
                }
                std::cout << "  done: " << ok << " ok, " << fail << " failed\n";
            }
            else {
                std::cout << "  unknown command or wrong args (type 'help')\n";
            }
        } catch (const std::exception& e) {
            std::cout << "  error: " << e.what() << "\n";
        }
    }

    std::cout << "bye\n";
}

// --- command-line ----------------------------------------------------------

static void printUsage(const char* prog) {
    std::cerr << "txtr - Texture Tool v1.0\n"
              << "Usage:\n"
              << "  " << prog << "                          - interactive mode\n"
              << "  " << prog << " info <file>              - show image info\n"
              << "  " << prog << " convert <in> <out>       - convert format\n"
              << "  " << prog << " resize <in> <out> WxH    - resize\n"
              << "  " << prog << " optimize <in> <out>      - optimize\n"
              << "  " << prog << " batch <dir> [outdir] [-f fmt]  - batch convert\n"
              << "Formats: png, jpg, bmp, tga, dds\n";
}

enum class Command { Info, Convert, Resize, Optimize, Batch, Interactive, None };

static Command parseCommand(const char* s) {
    if (std::strcmp(s, "info") == 0) return Command::Info;
    if (std::strcmp(s, "convert") == 0) return Command::Convert;
    if (std::strcmp(s, "resize") == 0) return Command::Resize;
    if (std::strcmp(s, "optimize") == 0) return Command::Optimize;
    if (std::strcmp(s, "batch") == 0) return Command::Batch;
    if (std::strcmp(s, "interactive") == 0) return Command::Interactive;
    return Command::None;
}

static int cmdBatch(int argc, char** argv) {
    if (argc < 3) { printUsage(argv[0]); return 1; }

    std::string inDir = argv[2];
    std::string outDir;
    txtr::ImageFormat outFmt = txtr::ImageFormat::DDS;
    bool useExtFormat = false;

    int i = 3;
    if (i < argc && argv[i][0] != '-') {
        outDir = argv[i++];
    }
    while (i < argc) {
        if (std::strcmp(argv[i], "-f") == 0 && i + 1 < argc) {
            outFmt = parseFormat(argv[i + 1]);
            if (outFmt != txtr::ImageFormat::Unknown) useExtFormat = true;
            i += 2;
        } else break;
    }

    auto files = listDir(inDir, true);
    if (files.empty()) {
        std::cerr << "no images found in " << inDir << "\n";
        return 1;
    }

    if (outDir.empty()) outDir = inDir + "/converted";
    if (!ensureDir(outDir)) {
        std::cerr << "cannot create output dir: " << outDir << "\n";
        return 1;
    }

    int ok = 0, fail = 0;
    for (auto& f : files) {
        auto ext = txtr::platform::getExtension(f);
        txtr::ImageFormat fmt = useExtFormat ? outFmt : parseFormat(ext.c_str());
        if (fmt == txtr::ImageFormat::Unknown) { ++fail; continue; }

        auto baseName = f.substr(f.rfind('/') + 1);
        auto outFile = outDir + "/" + replaceExt(baseName, formatExt(fmt));

        try {
            auto tex = txtr::load(f);
            txtr::save(outFile, tex, fmt);
            std::cout << "[+] " << baseName << " -> " << outFile << "\n";
            ++ok;
        } catch (const std::exception& e) {
            std::cout << "[X] " << baseName << ": " << e.what() << "\n";
            ++fail;
        }
    }

    std::cout << "\ndone: " << ok << " ok, " << fail << " failed\n";
    return (fail == 0) ? 0 : 1;
}

int main(int argc, char** argv) {
    if (argc < 2) {
        interactive();
        return 0;
    }

    auto cmd = parseCommand(argv[1]);

    if (cmd == Command::Interactive) {
        interactive();
        return 0;
    }

    switch (cmd) {
        case Command::Info: {
            if (argc < 3) { printUsage(argv[0]); return 1; }
            auto info = txtr::getInfo(argv[2]);
            std::cout << "File:     " << argv[2] << "\n"
                      << "Size:     " << info.width << "x" << info.height << "\n"
                      << "Channels: " << info.channels << "\n"
                      << "Bytes:    " << info.fileSize << "\n"
                      << "Alpha:    " << (info.hasAlpha ? "yes" : "no") << "\n";
            return 0;
        }
        case Command::Convert: {
            if (argc < 4) { printUsage(argv[0]); return 1; }
            try {
                auto tex = txtr::load(argv[2]);
                txtr::save(argv[3], tex);
                std::cout << "saved to " << argv[3] << "\n";
            } catch (const std::exception& e) {
                std::cerr << "error: " << e.what() << "\n";
                return 1;
            }
            return 0;
        }
        case Command::Resize: {
            if (argc < 5) { printUsage(argv[0]); return 1; }
            txtr::ResizeOptions opts;
            if (std::sscanf(argv[4], "%dx%d", &opts.newWidth, &opts.newHeight) != 2) {
                std::cerr << "bad dimensions: " << argv[4] << "  (use WxH, e.g. 256x256)\n";
                return 1;
            }
            try {
                auto tex = txtr::load(argv[2]);
                auto result = txtr::resize(tex, opts);
                txtr::save(argv[3], result);
                std::cout << tex.width << "x" << tex.height
                          << " -> " << result.width << "x" << result.height << "\n";
            } catch (const std::exception& e) {
                std::cerr << "error: " << e.what() << "\n";
                return 1;
            }
            return 0;
        }
        case Command::Optimize: {
            if (argc < 4) { printUsage(argv[0]); return 1; }
            txtr::OptimizeOptions opts;
            opts.generateMipmaps = true;
            opts.maxDimension = 1024;
            try {
                auto tex = txtr::load(argv[2]);
                auto result = txtr::optimize(tex, opts);
                txtr::save(argv[3], result);
                std::cout << tex.totalSize() << " -> " << result.totalSize() << " bytes\n";
            } catch (const std::exception& e) {
                std::cerr << "error: " << e.what() << "\n";
                return 1;
            }
            return 0;
        }
        case Command::Batch:
            return cmdBatch(argc, argv);
        default:
            printUsage(argv[0]);
            return 1;
    }
}
