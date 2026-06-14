#pragma once
#include <string>
#include <vector>
#include <unordered_set>
#include <unordered_map>

namespace ravex {

/**
 * NoGhostBlocks native engine.
 * Tracks known block states received from server to detect and reject
 * ghost blocks (client-side desync after fast mining/placing).
 */
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

    /**
     * Called when server sends a block update — registers the authoritative state.
     */
    void onServerBlockUpdate(int x, int y, int z, const std::string& blockId);

    /**
     * Called to check if a client-side block at (x,y,z) with given id is a ghost
     * (differs from what the server last told us).
     * Returns true if the block is a ghost and should be invisible/suppressed.
     */
    bool isGhostBlock(int x, int y, int z, const std::string& clientBlockId) const;

    /**
     * Called when client starts mining — marks block as "pending removal"
     * to prevent premature visual cleanup.
     */
    void markMiningStart(int x, int y, int z);
    void markMiningEnd(int x, int y, int z);

    /**
     * Forget all state (e.g. on world change).
     */
    void reset();

private:
    NoGhostBlocksEngine() = default;
    mutable std::unordered_map<BlockKey, std::string, BlockKeyHash> serverState;
    std::unordered_set<BlockKey, BlockKeyHash> miningSet;
};

} // namespace ravex
