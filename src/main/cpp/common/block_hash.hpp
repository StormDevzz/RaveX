#pragma once

#include <unordered_set>
#include <cstdint>

struct Vec3BlockHash {
    std::size_t operator()(const ravex::Vec3& v) const {
        std::size_t h1 = std::hash<int>()((int)v.x);
        std::size_t h2 = std::hash<int>()((int)v.y);
        std::size_t h3 = std::hash<int>()((int)v.z);
        return h1 ^ (h2 << 1) ^ (h3 << 2);
    }
};

struct Vec3BlockEq {
    bool operator()(const ravex::Vec3& a, const ravex::Vec3& b) const {
        return (int)a.x == (int)b.x && (int)a.y == (int)b.y && (int)a.z == (int)b.z;
    }
};

using BlockSet = std::unordered_set<ravex::Vec3, Vec3BlockHash, Vec3BlockEq>;