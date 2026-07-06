#pragma once
#include <string>
#include <vector>
#include <cstdint>

namespace ravex {
namespace fileprot {

class FileHasher {
public:
    FileHasher();
    std::string hashFile(const std::string& path);
    std::string hashData(const unsigned char* data, size_t len);
    std::string hashString(const std::string& input);
    bool verifyFile(const std::string& path, const std::string& expectedHash);

    static constexpr size_t BUFFER_SIZE = 65536;

private:
    void init();
    void update(const unsigned char* data, size_t len);
    std::string final();
    uint32_t s[8];
    uint32_t temp[8];
    unsigned char block[64];
    size_t blockLen;
    uint64_t bitCount;
};

}
}
