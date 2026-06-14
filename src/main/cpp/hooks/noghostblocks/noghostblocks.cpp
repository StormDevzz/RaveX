#include "noghostblocks.h"
#include <mutex>

namespace ravex {

static std::mutex ngb_mutex;

NoGhostBlocksEngine& NoGhostBlocksEngine::instance() {
    static NoGhostBlocksEngine eng;
    return eng;
}

void NoGhostBlocksEngine::onServerBlockUpdate(int x, int y, int z, const std::string& blockId) {
    std::lock_guard<std::mutex> lock(ngb_mutex);
    BlockKey key{x, y, z};
    if (blockId.empty() || blockId == "minecraft:air") {
        serverState.erase(key);
    } else {
        serverState[key] = blockId;
    }
}

bool NoGhostBlocksEngine::isGhostBlock(int x, int y, int z, const std::string& clientBlockId) const {
    std::lock_guard<std::mutex> lock(ngb_mutex);
    BlockKey key{x, y, z};

    // If being actively mined, don't flag as ghost
    if (miningSet.count(key)) return false;

    auto it = serverState.find(key);
    if (it == serverState.end()) {
        // Server never told us about this block — could be a ghost if client shows non-air
        // Only flag if client shows a solid block that the server never confirmed
        return !clientBlockId.empty() && clientBlockId != "minecraft:air";
    }

    // Server told us it's air but client shows something — ghost!
    // (or server shows different block)
    return it->second != clientBlockId;
}

void NoGhostBlocksEngine::markMiningStart(int x, int y, int z) {
    std::lock_guard<std::mutex> lock(ngb_mutex);
    miningSet.insert({x, y, z});
}

void NoGhostBlocksEngine::markMiningEnd(int x, int y, int z) {
    std::lock_guard<std::mutex> lock(ngb_mutex);
    miningSet.erase({x, y, z});
}

void NoGhostBlocksEngine::reset() {
    std::lock_guard<std::mutex> lock(ngb_mutex);
    serverState.clear();
    miningSet.clear();
}

} // namespace ravex
