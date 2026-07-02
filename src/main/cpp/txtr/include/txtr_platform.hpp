#pragma once

#include <cstdio>
#include <string>
#include <vector>
#include <fstream>
#include <algorithm>
#include <cctype>

#if defined(__linux__) || defined(__linux)
    #define TXTR_PLATFORM_LINUX 1
#elif defined(_WIN32) || defined(_WIN64)
    #define TXTR_PLATFORM_WINDOWS 1
#elif defined(__APPLE__) || defined(__MACH__)
    #define TXTR_PLATFORM_MACOS 1
#else
    #error "Unsupported platform"
#endif

#if defined(__x86_64__) || defined(_M_X64) || defined(__aarch64__)
    #define TXTR_ARCH_64BIT 1
#else
    #define TXTR_ARCH_32BIT 1
#endif

#if defined(__BYTE_ORDER__) && __BYTE_ORDER__ == __ORDER_BIG_ENDIAN__
    #define TXTR_BIG_ENDIAN 1
#else
    #define TXTR_LITTLE_ENDIAN 1
#endif

namespace txtr {
namespace platform {

inline std::vector<uint8_t> readFile(const std::string& path) {
    std::ifstream file(path, std::ios::binary | std::ios::ate);
    if (!file) return {};
    auto size = file.tellg();
    file.seekg(0, std::ios::beg);
    std::vector<uint8_t> buf(static_cast<size_t>(size));
    file.read(reinterpret_cast<char*>(buf.data()), size);
    if (!file) buf.clear();
    return buf;
}

inline bool writeFile(const std::string& path, const uint8_t* data, size_t size) {
    std::ofstream file(path, std::ios::binary);
    if (!file) return false;
    file.write(reinterpret_cast<const char*>(data), static_cast<std::streamsize>(size));
    return file.good();
}

inline bool fileExists(const std::string& path) {
    std::ifstream file(path);
    return file.good();
}

inline std::string getExtension(const std::string& path) {
    auto pos = path.rfind('.');
    if (pos == std::string::npos) return {};
    std::string ext = path.substr(pos + 1);
    for (auto& c : ext) c = static_cast<char>(std::tolower(c));
    return ext;
}

} 
} 
