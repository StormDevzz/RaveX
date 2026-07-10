#pragma once

#include <cstdint>
#include <vector>

namespace packet {
namespace compress {

std::vector<uint8_t> zlibCompress(const uint8_t* data, size_t len, int level = -1);
std::vector<uint8_t> zlibDecompress(const uint8_t* data, size_t len, size_t maxUncompressed = 0);

std::vector<uint8_t> compressPacket(const std::vector<uint8_t>& data, int threshold);
std::vector<uint8_t> decompressPacket(const std::vector<uint8_t>& data, int threshold);

}
}
