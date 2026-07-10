#pragma once

#include <cstdint>
#include <vector>
#include <cstring>
#include <string>

namespace model {

class Buffer {
public:
    Buffer() = default;
    explicit Buffer(std::vector<uint8_t> d) : data_(std::move(d)) {}
    explicit Buffer(const uint8_t* d, size_t s) : data_(d, d + s) {}

    const uint8_t* data() const { return data_.data(); }
    uint8_t* data() { return data_.data(); }
    size_t size() const { return data_.size(); }
    size_t tell() const { return pos_; }
    size_t remain() const { return data_.size() - pos_; }
    bool eof() const { return pos_ >= data_.size(); }

    void seek(size_t p) { pos_ = p; }
    void skip(size_t n) { pos_ += n; if (pos_ > data_.size()) pos_ = data_.size(); }

    uint8_t readU8() {
        if (pos_ + 1 > data_.size()) return 0;
        return data_[pos_++];
    }

    uint16_t readU16LE() {
        if (pos_ + 2 > data_.size()) return 0;
        uint16_t v = data_[pos_] | (data_[pos_+1] << 8);
        pos_ += 2;
        return v;
    }

    uint32_t readU32LE() {
        if (pos_ + 4 > data_.size()) return 0;
        uint32_t v = data_[pos_] | (data_[pos_+1] << 8) |
                    (data_[pos_+2] << 16) | (data_[pos_+3] << 24);
        pos_ += 4;
        return v;
    }

    float readFloat() {
        uint32_t v = readU32LE();
        float f;
        std::memcpy(&f, &v, sizeof(f));
        return f;
    }

    std::string readString(size_t len) {
        if (pos_ + len > data_.size()) len = data_.size() - pos_;
        std::string s(reinterpret_cast<const char*>(data_.data() + pos_), len);
        pos_ += len;
        return s;
    }

    void writeU8(uint8_t v) { data_.push_back(v); }
    void writeU16LE(uint16_t v) {
        data_.push_back(v & 0xFF); data_.push_back((v >> 8) & 0xFF);
    }
    void writeU32LE(uint32_t v) {
        data_.push_back(v & 0xFF); data_.push_back((v >> 8) & 0xFF);
        data_.push_back((v >> 16) & 0xFF); data_.push_back((v >> 24) & 0xFF);
    }
    void writeFloat(float f) {
        uint32_t v; std::memcpy(&v, &f, sizeof(v)); writeU32LE(v);
    }
    void writeString(const std::string& s) {
        data_.insert(data_.end(), s.begin(), s.end());
    }
    void write(const uint8_t* d, size_t s) {
        data_.insert(data_.end(), d, d + s);
    }

    std::vector<uint8_t> take() { pos_ = 0; return std::move(data_); }

private:
    std::vector<uint8_t> data_;
    size_t pos_ = 0;
};

}
