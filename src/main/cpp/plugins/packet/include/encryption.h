#pragma once

#include <cstdint>
#include <vector>
#include <string>

namespace packet {
namespace crypto {

struct Key {
    std::vector<uint8_t> data;
    size_t size() const { return data.size(); }
};

struct KeyPair {
    Key publicKey;
    Key privateKey;
};

KeyPair generateKeyPair();
Key generateSecret();

std::vector<uint8_t> encrypt(const uint8_t* data, size_t len, const Key& key);
std::vector<uint8_t> decrypt(const uint8_t* data, size_t len, const Key& key);
std::vector<uint8_t> hash(const uint8_t* data, size_t len);
std::vector<uint8_t> hash(const std::vector<uint8_t>& a, const std::vector<uint8_t>& b);

} // namespace crypto
} // namespace packet
