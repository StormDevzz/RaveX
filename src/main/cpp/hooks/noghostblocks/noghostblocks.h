#pragma once
#include <string>
#include <vector>
#include <unordered_set>
#include <unordered_map>

namespace ravex {


struct BlockKey {
    int x, y, z;
    bool operator==(const BlockKey& o) const {
        return x == o.x && y == o.y && z == o.z;
    }
};

struct BlockKeyHash {
    std::size_t operator()(const BlockKey& k) const {
        std::size_t h = 0;
        h ^= std::hash<int>()(k.x) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= std::hash<int>()(k.y) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= std::hash<int>()(k.z) + 0x9e3779b9 + (h << 6) + (h >> 2);
        return h;
    }
};

class NoGhostBlocksEngine {
public:
    static NoGhostBlocksEngine& instance();

    
    void onServerBlockUpdate(int x, int y, int z, const std::string& blockId);

    
    bool isGhostBlock(int x, int y, int z, const std::string& clientBlockId) const;

    
    void markMiningStart(int x, int y, int z);
    void markMiningEnd(int x, int y, int z);

    
    void reset();

private:
    NoGhostBlocksEngine() = default;
    mutable std::unordered_map<BlockKey, std::string, BlockKeyHash> serverState;
    std::unordered_set<BlockKey, BlockKeyHash> miningSet;
};

} 
