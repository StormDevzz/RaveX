#pragma once

#include <string>
#include <vector>
#include <fstream>
#include <algorithm>
#include <cctype>
#include <cstdio>

namespace model {
namespace platform {

inline std::vector<uint8_t> readFile(const std::string& path) {
    std::ifstream f(path, std::ios::binary | std::ios::ate);
    if (!f) return {};
    auto sz = f.tellg();
    f.seekg(0);
    std::vector<uint8_t> buf(static_cast<size_t>(sz));
    f.read(reinterpret_cast<char*>(buf.data()), sz);
    return buf;
}

inline bool writeFile(const std::string& path, const uint8_t* d, size_t sz) {
    std::ofstream f(path, std::ios::binary);
    if (!f) return false;
    f.write(reinterpret_cast<const char*>(d), static_cast<std::streamsize>(sz));
    return f.good();
}

inline bool writeString(const std::string& path, const std::string& s) {
    return writeFile(path, reinterpret_cast<const uint8_t*>(s.data()), s.size());
}

inline std::string getExt(const std::string& path) {
    auto p = path.rfind('.');
    if (p == std::string::npos) return {};
    auto e = path.substr(p + 1);
    for (auto& c : e) c = static_cast<char>(std::tolower(c));
    return e;
}

inline std::string dirName(const std::string& path) {
    auto p = path.rfind('/');
    return (p == std::string::npos) ? "." : path.substr(0, p);
}

} 
} 
