#pragma once
#include <vector>
#include <cmath>
#include <cstdint>

// ─── Block position ──────────────────────────────────────────────────────────
struct TntBlockPos {
    int x, y, z;

    bool operator==(const TntBlockPos& o) const {
        return x == o.x && y == o.y && z == o.z;
    }
    bool operator!=(const TntBlockPos& o) const { return !(*this == o); }
};

// ─── Configuration passed from Java ──────────────────────────────────────────
struct TntAuraConfig {
    double range;
    bool   roof;
    bool   includeBottom;    // place below target feet too
    int    gapDirection;     // 0=auto, 1=north, 2=south, 3=east, 4=west
};

// ─── Placement result for a single block ─────────────────────────────────────
struct TntPlacement {
    bool       valid;
    TntBlockPos neighbor;     // existing solid block to click against
    int        face;          // Direction ordinal (DOWN=0 UP=1 N=2 S=3 W=4 E=5)
    TntBlockPos targetBlock;  // the air position where new block will appear
};

// ─── Full cage result ────────────────────────────────────────────────────────
struct TntAuraResult {
    std::vector<TntPlacement> cagePlacements;   // ordered obsidian placements
    TntPlacement              tntPlacement;      // where to place TNT
    TntBlockPos               tntPos;            // TNT block position
    TntBlockPos               gapPos;            // the gap left in the cage
    bool                      cageComplete;      // true = all cage slots filled
    int                       totalCageSlots;
    int                       filledCageSlots;
};

// ─── Core functions ──────────────────────────────────────────────────────────

/**
 * Calculate the full cage layout around a target and find the optimal
 * TNT gap + placement position.
 */
TntAuraResult calculateTntAuraCage(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    const std::vector<TntBlockPos>& solidBlocks,
    const TntAuraConfig& config
);

/**
 * Given a cage that is already (partially) built, find the next block
 * that needs to be placed and return the placement info.
 */
TntPlacement findNextCagePlacement(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    const std::vector<TntBlockPos>& solidBlocks,
    const TntAuraConfig& config,
    const TntBlockPos& gapPos
);

/**
 * Calculate where to place TNT through the gap after the cage is built.
 */
TntPlacement calculateTntPlacement(
    double playerX, double playerY, double playerZ,
    const TntBlockPos& gapPos,
    const std::vector<TntBlockPos>& solidBlocks,
    double range
);
