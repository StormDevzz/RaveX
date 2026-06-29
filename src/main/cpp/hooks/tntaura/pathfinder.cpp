#include "pathfinder.h"
#include <algorithm>
#include <unordered_set>
#include <cmath>

struct PFBlockHash {
    size_t operator()(const TntBlockPos& p) const {
        size_t h = 0;
        h ^= std::hash<int>()(p.x) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= std::hash<int>()(p.y) + 0x9e3779b9 + (h << 6) + (h >> 2);
        h ^= std::hash<int>()(p.z) + 0x9e3779b9 + (h << 6) + (h >> 2);
        return h;
    }
};

using PFBlockSet = std::unordered_set<TntBlockPos, PFBlockHash>;

static const TntBlockPos PF_OFFSETS[6] = {
    {0,-1,0}, {0,1,0}, {0,0,-1}, {0,0,1}, {-1,0,0}, {1,0,0}
};

static double pfDistSqr(double px, double py, double pz, const TntBlockPos& b) {
    double cx = b.x + 0.5, cy = b.y + 0.5, cz = b.z + 0.5;
    double dx = px - cx, dy = (py + 1.62) - cy, dz = pz - cz;
    return dx*dx + dy*dy + dz*dz;
}

static int countSolidNeighbors(const TntBlockPos& pos, const PFBlockSet& solids) {
    int count = 0;
    for (const auto& off : PF_OFFSETS) {
        TntBlockPos n = {pos.x + off.x, pos.y + off.y, pos.z + off.z};
        if (solids.count(n)) count++;
    }
    return count;
}

std::vector<PlacementPriority> computePlacementOrder(
    const std::vector<TntBlockPos>& cageCandidates,
    const std::vector<TntBlockPos>& solidBlocks,
    double playerX, double playerY, double playerZ
) {
    PFBlockSet solids(solidBlocks.begin(), solidBlocks.end());
    std::vector<PlacementPriority> result;

    for (const auto& c : cageCandidates) {
        if (solids.count(c)) continue; 

        double dist = pfDistSqr(playerX, playerY, playerZ, c);
        int neighbors = countSolidNeighbors(c, solids);

        
        
        double score = 0.0;
        if (neighbors == 0) score += 1000.0; 
        score += dist * 0.1;                 
        score -= neighbors * 50.0;           
        score -= c.y * 10.0;                 

        result.push_back({c, score, neighbors == 0});
    }

    std::sort(result.begin(), result.end(), [](const PlacementPriority& a, const PlacementPriority& b) {
        return a.score < b.score;
    });

    return result;
}

std::vector<TntBlockPos> computeSupportChain(
    const TntBlockPos& target,
    const std::vector<TntBlockPos>& solidBlocks,
    double playerX, double playerY, double playerZ,
    double range,
    int maxDepth
) {
    PFBlockSet solids(solidBlocks.begin(), solidBlocks.end());
    std::vector<TntBlockPos> chain;
    double rSqr = range * range;

    
    TntBlockPos current = target;
    for (int depth = 0; depth < maxDepth; depth++) {
        bool hasNeighbor = false;
        for (const auto& off : PF_OFFSETS) {
            TntBlockPos n = {current.x + off.x, current.y + off.y, current.z + off.z};
            if (solids.count(n)) {
                hasNeighbor = true;
                break;
            }
        }

        if (hasNeighbor) break;

        
        TntBlockPos below = {current.x, current.y - 1, current.z};
        if (pfDistSqr(playerX, playerY, playerZ, below) > rSqr) break;

        chain.insert(chain.begin(), below); 
        current = below;
    }

    chain.push_back(target);
    return chain;
}
