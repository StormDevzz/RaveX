#pragma once
#include "tntaura.h"
#include <vector>

// ─── Placement order priority ────────────────────────────────────────────────

struct PlacementPriority {
    TntBlockPos pos;
    double      score;   // lower = place first
    bool        isSupport;
};

/**
 * Sort cage positions by optimal placement order:
 *  - bottom/support blocks first
 *  - closest to player for reachability
 *  - blocks with existing solid neighbors prioritized
 */
std::vector<PlacementPriority> computePlacementOrder(
    const std::vector<TntBlockPos>& cageCandidates,
    const std::vector<TntBlockPos>& solidBlocks,
    double playerX, double playerY, double playerZ
);

/**
 * Find the support chain: if a cage block has no solid neighbor,
 * compute intermediate support blocks needed to reach it.
 * Returns the ordered list of blocks to place (supports first, then target).
 */
std::vector<TntBlockPos> computeSupportChain(
    const TntBlockPos& target,
    const std::vector<TntBlockPos>& solidBlocks,
    double playerX, double playerY, double playerZ,
    double range,
    int maxDepth = 3
);
