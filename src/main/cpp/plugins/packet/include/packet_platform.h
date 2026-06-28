#pragma once

#include <string>
#include <vector>
#include <fstream>
#include <cstdlib>
#include <ctime>
#include <unistd.h>

namespace packet {
namespace platform {

inline std::string timestamp() {
    char buf[64];
    auto t = std::time(nullptr);
    std::strftime(buf, sizeof(buf), "%H:%M:%S", std::localtime(&t));
    return buf;
}

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

inline bool isRoot() {
    return geteuid() == 0;
}

} // namespace platform
} // namespace packet
