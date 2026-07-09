#pragma once

#include "packet_types.hpp"
#include "packet_utils.hpp"
#include <cstdint>
#include <vector>
#include <cstring>
#include <algorithm>

namespace packet {

inline int varintSize(int32_t v) {
    int n = 0;
    do { ++n; v >>= 7; } while (v);
    return n;
}

inline int readVarint(const uint8_t* data, size_t size, int32_t& result) {
    result = 0;
    int shift = 0;
    size_t pos = 0;
    while (pos < size) {
        uint8_t b = data[pos++];
        result |= (b & 0x7F) << shift;
        shift += 7;
        if (!(b & 0x80)) return static_cast<int>(pos);
    }
    return -1;
}

inline int writeVarint(uint8_t* buf, int32_t v) {
    int n = 0;
    while (true) {
        uint8_t b = v & 0x7F;
        v >>= 7;
        if (v) b |= 0x80;
        buf[n++] = b;
        if (!v) break;
    }
    return n;
}

inline int readVarint(const std::vector<uint8_t>& data, size_t offset, int32_t& result) {
    if (offset >= data.size()) return -1;
    return readVarint(data.data() + offset, data.size() - offset, result);
}

inline std::vector<uint8_t> writeVarintBuf(int32_t v) {
    std::vector<uint8_t> buf(varintSize(v));
    writeVarint(buf.data(), v);
    return buf;
}

struct PacketReader {
    const uint8_t* data;
    size_t size;
    size_t pos = 0;

    PacketReader(const uint8_t* d, size_t s) : data(d), size(s) {}

    int32_t readVar() { int32_t r; int n = readVarint(data + pos, size - pos, r); if (n > 0) pos += n; return r; }
    uint8_t readU8() { if (pos + 1 > size) return 0; return data[pos++]; }
    uint16_t readU16() { if (pos + 2 > size) return 0; uint16_t v = utils::readU16BE(data + pos); pos += 2; return v; }
    uint32_t readU32() { if (pos + 4 > size) return 0; uint32_t v = utils::readU32BE(data + pos); pos += 4; return v; }
    uint64_t readU64() { if (pos + 8 > size) return 0; uint64_t v = utils::readU64BE(data + pos); pos += 8; return v; }
    float readFloat() { uint32_t v = readU32(); float f; std::memcpy(&f, &v, 4); return f; }
    double readDouble() { uint64_t v = readU64(); double d; std::memcpy(&d, &v, 8); return d; }

    std::string readString() {
        int32_t len = readVar();
        if (pos + static_cast<size_t>(len) > size) return {};
        std::string s(reinterpret_cast<const char*>(data + pos), len);
        pos += len;
        return s;
    }

    std::vector<uint8_t> readBytes(int32_t len) {
        if (pos + len > size) len = static_cast<int32_t>(size - pos);
        std::vector<uint8_t> b(data + pos, data + pos + len);
        pos += len;
        return b;
    }
};

struct PacketWriter {
    std::vector<uint8_t> buf;

    void writeVar(int32_t v) { auto b = writeVarintBuf(v); buf.insert(buf.end(), b.begin(), b.end()); }
    void writeU8(uint8_t v) { buf.push_back(v); }
    void writeU16(uint16_t v) { buf.push_back((v >> 8) & 0xFF); buf.push_back(v & 0xFF); }
    void writeU32(uint32_t v) { buf.push_back((v >> 24) & 0xFF); buf.push_back((v >> 16) & 0xFF); buf.push_back((v >> 8) & 0xFF); buf.push_back(v & 0xFF); }
    void writeU64(uint64_t v) {
        for (int i = 7; i >= 0; --i) buf.push_back((v >> (i * 8)) & 0xFF);
    }
    void writeFloat(float f) { uint32_t v; std::memcpy(&v, &f, 4); writeU32(v); }
    void writeDouble(double d) { uint64_t v; std::memcpy(&v, &d, 8); writeU64(v); }
    void writeString(const std::string& s) { writeVar(static_cast<int32_t>(s.size())); buf.insert(buf.end(), s.begin(), s.end()); }
    void writeBytes(const uint8_t* d, size_t s) { buf.insert(buf.end(), d, d + s); }
};

} 
