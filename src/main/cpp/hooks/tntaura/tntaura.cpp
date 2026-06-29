#include "tntaura.h"
#include <cmath>
#include <vector>
#include <algorithm>
#include <unordered_set>



struct TntBlockPosHash {
    size_t operator()(const TntBlockPos& p) const {
        size_t h = 0;
        h ^= std::hash<int>()(p.x) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= std::hash<int>()(p.y) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= std::hash<int>()(p.z) + 0x9e3779b9 + (h << 6) + (h >> 2);
        return h;
    }
};

using BlockSet = std::unordered_set<TntBlockPos, TntBlockPosHash>;

static int getFace(const TntBlockPos& from, const TntBlockPos& to) {
    int dx = to.x - from.x;
    int dy = to.y - from.y;
    int dz = to.z - from.z;
    if (dy ==  1) return 1; 
    if (dy == -1) return 0; 
    if (dz == -1) return 2; 
    if (dz ==  1) return 3; 
    if (dx == -1) return 4; 
    if (dx ==  1) return 5; 
    return 1;
}

static double distSqr(double px, double py, double pz, const TntBlockPos& b) {
    double cx = b.x + 0.5, cy = b.y + 0.5, cz = b.z + 0.5;
    double dx = px - cx, dy = (py + 1.62) - cy, dz = pz - cz;
    return dx*dx + dy*dy + dz*dz;
}

static const TntBlockPos OFFSETS[6] = {
    {0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
};

static TntPlacement findPlacementFor(
    const TntBlockPos& target,
    double px, double py, double pz,
    const BlockSet& solids,
    double range
) {
    double rSqr = range * range;
    if (distSqr(px, py, pz, target) > rSqr)
        return {false, {0,0,0}, 0, {0,0,0}};

    for (const auto& off : OFFSETS) {
        TntBlockPos n = {target.x + off.x, target.y + off.y, target.z + off.z};
        if (solids.count(n)) {
            return {true, n, getFace(n, target), target};
        }
    }

    
    TntBlockPos below = {target.x, target.y - 1, target.z};
    if (!solids.count(below) && distSqr(px, py, pz, below) <= rSqr) {
        for (const auto& off : OFFSETS) {
            TntBlockPos n = {below.x + off.x, below.y + off.y, below.z + off.z};
            if (solids.count(n)) {
                return {true, n, getFace(n, below), below};
            }
        }
    }

    return {false, {0,0,0}, 0, {0,0,0}};
}


static TntPlacement findPlacementChain(
    const TntBlockPos& target,
    double px, double py, double pz,
    BlockSet& extendedSolids,
    double range
) {
    
    TntPlacement direct = findPlacementFor(target, px, py, pz, extendedSolids, range);
    if (direct.valid) return direct;

    
    TntBlockPos below = {target.x, target.y - 1, target.z};
    if (!extendedSolids.count(below) && distSqr(px, py, pz, below) <= range * range) {
        TntPlacement support = findPlacementFor(below, px, py, pz, extendedSolids, range);
        if (support.valid) {
            extendedSolids.insert(below);
            
            TntPlacement retry = findPlacementFor(target, px, py, pz, extendedSolids, range);
            if (retry.valid) return retry;
        }
    }

    return {false, {0,0,0}, 0, {0,0,0}};
}


static TntBlockPos selectGap(
    const TntBlockPos& feet,
    double playerX, double playerZ,
    int gapDir
) {
    
    int headY = feet.y + 1;
    if (gapDir == 1) return {feet.x, headY, feet.z - 1}; 
    if (gapDir == 2) return {feet.x, headY, feet.z + 1}; 
    if (gapDir == 3) return {feet.x + 1, headY, feet.z}; 
    if (gapDir == 4) return {feet.x - 1, headY, feet.z}; 

    
    double dx = playerX - (feet.x + 0.5);
    double dz = playerZ - (feet.z + 0.5);

    if (std::abs(dx) >= std::abs(dz)) {
        return dx > 0 ? TntBlockPos{feet.x + 1, headY, feet.z}
                      : TntBlockPos{feet.x - 1, headY, feet.z};
    } else {
        return dz > 0 ? TntBlockPos{feet.x, headY, feet.z + 1}
                      : TntBlockPos{feet.x, headY, feet.z - 1};
    }
}


static std::vector<TntBlockPos> buildCageCandidates(
    const TntBlockPos& feet, bool roof, bool bottom, const TntBlockPos& gap
) {
    std::vector<TntBlockPos> cands;
    
    TntBlockPos feetSides[4] = {
        {feet.x, feet.y, feet.z - 1},
        {feet.x, feet.y, feet.z + 1},
        {feet.x + 1, feet.y, feet.z},
        {feet.x - 1, feet.y, feet.z}
    };
    for (auto& c : feetSides) cands.push_back(c);

    
    TntBlockPos headSides[4] = {
        {feet.x, feet.y + 1, feet.z - 1},
        {feet.x, feet.y + 1, feet.z + 1},
        {feet.x + 1, feet.y + 1, feet.z},
        {feet.x - 1, feet.y + 1, feet.z}
    };
    for (auto& c : headSides) {
        if (c != gap) cands.push_back(c);
    }

    
    if (roof) {
        cands.push_back({feet.x, feet.y + 2, feet.z});
    }

    
    if (bottom) {
        cands.push_back({feet.x, feet.y - 1, feet.z});
    }

    return cands;
}



TntAuraResult calculateTntAuraCage(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    const std::vector<TntBlockPos>& solidBlocks,
    const TntAuraConfig& config
) {
    BlockSet solids(solidBlocks.begin(), solidBlocks.end());
    TntBlockPos feet = {
        (int)std::floor(targetX),
        (int)std::floor(targetY),
        (int)std::floor(targetZ)
    };

    TntBlockPos gap = selectGap(feet, playerX, playerZ, config.gapDirection);

    auto candidates = buildCageCandidates(feet, config.roof, config.includeBottom, gap);

    TntAuraResult result;
    result.gapPos = gap;
    result.totalCageSlots = (int)candidates.size();
    result.filledCageSlots = 0;
    result.cageComplete = false;

    
    BlockSet extendedSolids = solids;
    for (const auto& c : candidates) {
        if (solids.count(c)) {
            result.filledCageSlots++;
            continue;
        }
        TntPlacement p = findPlacementChain(c, playerX, playerY, playerZ, extendedSolids, config.range);
        if (p.valid) {
            result.cagePlacements.push_back(p);
            extendedSolids.insert(c);
        }
    }

    result.cageComplete = (result.filledCageSlots + (int)result.cagePlacements.size() == result.totalCageSlots);

    
    TntBlockPos tntTarget = gap;
    result.tntPos = tntTarget;
    result.tntPlacement = findPlacementChain(tntTarget, playerX, playerY, playerZ, extendedSolids, config.range);

    return result;
}

TntPlacement findNextCagePlacement(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    const std::vector<TntBlockPos>& solidBlocks,
    const TntAuraConfig& config,
    const TntBlockPos& gapPos
) {
    BlockSet solids(solidBlocks.begin(), solidBlocks.end());
    TntBlockPos feet = {
        (int)std::floor(targetX),
        (int)std::floor(targetY),
        (int)std::floor(targetZ)
    };

    auto candidates = buildCageCandidates(feet, config.roof, config.includeBottom, gapPos);

    BlockSet mutableSolids = solids;
    for (const auto& c : candidates) {
        if (solids.count(c)) continue;
        TntPlacement p = findPlacementChain(c, playerX, playerY, playerZ, mutableSolids, config.range);
        if (p.valid) return p;
    }

    return {false, {0,0,0}, 0, {0,0,0}};
}

TntPlacement calculateTntPlacement(
    double playerX, double playerY, double playerZ,
    const TntBlockPos& gapPos,
    const std::vector<TntBlockPos>& solidBlocks,
    double range
) {
    BlockSet solids(solidBlocks.begin(), solidBlocks.end());
    BlockSet mutableSolids = solids;
    return findPlacementChain(gapPos, playerX, playerY, playerZ, mutableSolids, range);
}
