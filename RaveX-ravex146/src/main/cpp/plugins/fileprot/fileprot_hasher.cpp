#include "include/fileprot_hasher.hpp"
#include <fstream>
#include <sstream>
#include <iomanip>
#include <cstring>

namespace ravex {
namespace fileprot {

FileHasher::FileHasher() {
    init();
}

void FileHasher::init() {
    s[0] = 0x6a09e667;
    s[1] = 0xbb67ae85;
    s[2] = 0x3c6ef372;
    s[3] = 0xa54ff53a;
    s[4] = 0x510e527f;
    s[5] = 0x9b05688c;
    s[6] = 0x1f83d9ab;
    s[7] = 0x5be0cd19;
    blockLen = 0;
    bitCount = 0;
}

static inline uint32_t rotr(uint32_t x, int n) {
    return (x >> n) | (x << (32 - n));
}

static inline uint32_t ch(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (~x & z);
}

static inline uint32_t maj(uint32_t x, uint32_t y, uint32_t z) {
    return (x & y) ^ (x & z) ^ (y & z);
}

static inline uint32_t sig0(uint32_t x) {
    return rotr(x, 2) ^ rotr(x, 13) ^ rotr(x, 22);
}

static inline uint32_t sig1(uint32_t x) {
    return rotr(x, 6) ^ rotr(x, 11) ^ rotr(x, 25);
}

static inline uint32_t gam0(uint32_t x) {
    return rotr(x, 7) ^ rotr(x, 18) ^ (x >> 3);
}

static inline uint32_t gam1(uint32_t x) {
    return rotr(x, 17) ^ rotr(x, 19) ^ (x >> 10);
}

static const uint32_t K[64] = {
    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
    0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
    0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
    0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
    0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
    0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
    0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
    0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
    0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
};

void FileHasher::update(const unsigned char* data, size_t len) {
    bitCount += len * 8;

    if (blockLen > 0) {
        size_t space = 64 - blockLen;
        if (len < space) {
            memcpy(block + blockLen, data, len);
            blockLen += len;
            return;
        }
        memcpy(block + blockLen, data, space);
        len -= space;
        data += space;

        uint32_t w[64];
        for (int i = 0; i < 16; i++) {
            w[i] = (block[i*4] << 24) | (block[i*4+1] << 16) |
                   (block[i*4+2] << 8) | block[i*4+3];
        }
        for (int i = 16; i < 64; i++) {
            w[i] = gam1(w[i-2]) + w[i-7] + gam0(w[i-15]) + w[i-16];
        }

        memcpy(temp, s, sizeof(s));
        for (int i = 0; i < 64; i++) {
            uint32_t S1 = sig1(temp[4]);
            uint32_t chVal = ch(temp[4], temp[5], temp[6]);
            uint32_t t1 = temp[7] + S1 + chVal + K[i] + w[i];
            uint32_t S0 = sig0(temp[0]);
            uint32_t majVal = maj(temp[0], temp[1], temp[2]);
            uint32_t t2 = S0 + majVal;
            temp[7] = temp[6];
            temp[6] = temp[5];
            temp[5] = temp[4];
            temp[4] = temp[3] + t1;
            temp[3] = temp[2];
            temp[2] = temp[1];
            temp[1] = temp[0];
            temp[0] = t1 + t2;
        }
        for (int i = 0; i < 8; i++) s[i] += temp[i];

        blockLen = 0;
    }

    while (len >= 64) {
        uint32_t w[64];
        for (int i = 0; i < 16; i++) {
            w[i] = (data[i*4] << 24) | (data[i*4+1] << 16) |
                   (data[i*4+2] << 8) | data[i*4+3];
        }
        for (int i = 16; i < 64; i++) {
            w[i] = gam1(w[i-2]) + w[i-7] + gam0(w[i-15]) + w[i-16];
        }

        memcpy(temp, s, sizeof(s));
        for (int i = 0; i < 64; i++) {
            uint32_t S1 = sig1(temp[4]);
            uint32_t chVal = ch(temp[4], temp[5], temp[6]);
            uint32_t t1 = temp[7] + S1 + chVal + K[i] + w[i];
            uint32_t S0 = sig0(temp[0]);
            uint32_t majVal = maj(temp[0], temp[1], temp[2]);
            uint32_t t2 = S0 + majVal;
            temp[7] = temp[6];
            temp[6] = temp[5];
            temp[5] = temp[4];
            temp[4] = temp[3] + t1;
            temp[3] = temp[2];
            temp[2] = temp[1];
            temp[1] = temp[0];
            temp[0] = t1 + t2;
        }
        for (int i = 0; i < 8; i++) s[i] += temp[i];

        len -= 64;
        data += 64;
    }

    if (len > 0) {
        memcpy(block, data, len);
        blockLen = len;
    }
}

std::string FileHasher::final() {
    block[blockLen++] = 0x80;
    if (blockLen > 56) {
        memset(block + blockLen, 0, 64 - blockLen);
        uint32_t w[64];
        for (int i = 0; i < 16; i++) {
            w[i] = (block[i*4] << 24) | (block[i*4+1] << 16) |
                   (block[i*4+2] << 8) | block[i*4+3];
        }
        for (int i = 16; i < 64; i++) {
            w[i] = gam1(w[i-2]) + w[i-7] + gam0(w[i-15]) + w[i-16];
        }
        memcpy(temp, s, sizeof(s));
        for (int i = 0; i < 64; i++) {
            uint32_t S1 = sig1(temp[4]);
            uint32_t chVal = ch(temp[4], temp[5], temp[6]);
            uint32_t t1 = temp[7] + S1 + chVal + K[i] + w[i];
            uint32_t S0 = sig0(temp[0]);
            uint32_t majVal = maj(temp[0], temp[1], temp[2]);
            uint32_t t2 = S0 + majVal;
            temp[7] = temp[6];
            temp[6] = temp[5];
            temp[5] = temp[4];
            temp[4] = temp[3] + t1;
            temp[3] = temp[2];
            temp[2] = temp[1];
            temp[1] = temp[0];
            temp[0] = t1 + t2;
        }
        for (int i = 0; i < 8; i++) s[i] += temp[i];
        blockLen = 0;
    }

    memset(block + blockLen, 0, 56 - blockLen);
    uint64_t bits = bitCount;
    block[56] = (bits >> 56) & 0xFF;
    block[57] = (bits >> 48) & 0xFF;
    block[58] = (bits >> 40) & 0xFF;
    block[59] = (bits >> 32) & 0xFF;
    block[60] = (bits >> 24) & 0xFF;
    block[61] = (bits >> 16) & 0xFF;
    block[62] = (bits >> 8) & 0xFF;
    block[63] = bits & 0xFF;

    uint32_t w[64];
    for (int i = 0; i < 16; i++) {
        w[i] = (block[i*4] << 24) | (block[i*4+1] << 16) |
               (block[i*4+2] << 8) | block[i*4+3];
    }
    for (int i = 16; i < 64; i++) {
        w[i] = gam1(w[i-2]) + w[i-7] + gam0(w[i-15]) + w[i-16];
    }

    memcpy(temp, s, sizeof(s));
    for (int i = 0; i < 64; i++) {
        uint32_t S1 = sig1(temp[4]);
        uint32_t chVal = ch(temp[4], temp[5], temp[6]);
        uint32_t t1 = temp[7] + S1 + chVal + K[i] + w[i];
        uint32_t S0 = sig0(temp[0]);
        uint32_t majVal = maj(temp[0], temp[1], temp[2]);
        uint32_t t2 = S0 + majVal;
        temp[7] = temp[6];
        temp[6] = temp[5];
        temp[5] = temp[4];
        temp[4] = temp[3] + t1;
        temp[3] = temp[2];
        temp[2] = temp[1];
        temp[1] = temp[0];
        temp[0] = t1 + t2;
    }
    for (int i = 0; i < 8; i++) s[i] += temp[i];

    std::stringstream ss;
    for (int i = 0; i < 8; i++) {
        ss << std::hex << std::setw(8) << std::setfill('0') << s[i];
    }
    return ss.str();
}

std::string FileHasher::hashFile(const std::string& path) {
    std::ifstream file(path, std::ios::binary);
    if (!file.is_open()) return "";

    init();
    std::vector<char> buf(BUFFER_SIZE);
    while (file.read(buf.data(), BUFFER_SIZE) || file.gcount() > 0) {
        update(reinterpret_cast<const unsigned char*>(buf.data()), file.gcount());
    }
    return final();
}

std::string FileHasher::hashData(const unsigned char* data, size_t len) {
    init();
    update(data, len);
    return final();
}

std::string FileHasher::hashString(const std::string& input) {
    return hashData(reinterpret_cast<const unsigned char*>(input.c_str()), input.length());
}

bool FileHasher::verifyFile(const std::string& path, const std::string& expectedHash) {
    return hashFile(path) == expectedHash;
}

}
}
