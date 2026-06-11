#include "baseplace.h"
#include "../autocrystal/entity_tracker.h"
#include <cmath>
#include <algorithm>
#include <limits>

namespace ravex {

static bool intersectsEntity(double ex, double ey, double ez, int bx, int by, int bz) {
    // Standard player/entity bounding box width of 0.6 (radius 0.3) and height 1.8
    double minX = ex - 0.3;
    double maxX = ex + 0.3;
    double minY = ey;
    double maxY = ey + 1.8;
    double minZ = ez - 0.3;
    double maxZ = ez + 0.3;

    double bMinX = bx;
    double bMaxX = bx + 1.0;
    double bMinY = by;
    double bMaxY = by + 1.0;
    double bMinZ = bz;
    double bMaxZ = bz + 1.0;

    return (bMaxX > minX && bMinX < maxX &&
            bMaxY > minY && bMinY < maxY &&
            bMaxZ > minZ && bMinZ < maxZ);
}

static int getFaceIndex(const Vec3& neighbor, const Vec3& candidate) {
    int dx = (int)std::floor(candidate.x) - (int)std::floor(neighbor.x);
    int dy = (int)std::floor(candidate.y) - (int)std::floor(neighbor.y);
    int dz = (int)std::floor(candidate.z) - (int)std::floor(neighbor.z);
    if (dy == 1) return 1;  // UP
    if (dy == -1) return 0; // DOWN
    if (dz == -1) return 2; // NORTH
    if (dz == 1) return 3;  // SOUTH
    if (dx == -1) return 4; // WEST
    if (dx == 1) return 5;  // EAST
    return 1; // Fallback
}

BasePlaceResult BasePlaceMath::findBestBasePlace(
    const Vec3&               playerPos,
    double                    playerHealth,
    double                    playerAbsorption,
    const EntityStats&        playerStats,
    const Vec3&               targetPos,
    double                    targetHealth,
    double                    targetAbsorption,
    const EntityStats&        targetStats,
    const std::vector<Vec3>&  solidBlocks,
    double                    placeRange,
    double                    targetRange,
    double                    minTargetDamage,
    double                    maxSelfDamage,
    double                    selfDamageWeight,
    bool                      antiSuicide,
    double                    antiSuicideMinHp,
    double                    predictTicks,
    bool                      airPlace
) {
    BasePlaceResult bestResult;
    bestResult.valid = false;
    bestResult.score = -std::numeric_limits<double>::infinity();

    Vec3 predictedTargetPos = EntityTracker::predictPosition(
        targetPos, targetStats.motionX, targetStats.motionY, targetStats.motionZ, predictTicks
    );

    int startX = (int)std::floor(predictedTargetPos.x) - 3;
    int endX = (int)std::floor(predictedTargetPos.x) + 3;
    int startY = (int)std::floor(predictedTargetPos.y) - 2;
    int endY = (int)std::floor(predictedTargetPos.y) + 2;
    int startZ = (int)std::floor(predictedTargetPos.z) - 3;
    int endZ = (int)std::floor(predictedTargetPos.z) + 3;

    auto isSolid = [&](const Vec3& pos) -> bool {
        for (const auto& sb : solidBlocks) {
            if ((int)std::floor(sb.x) == (int)std::floor(pos.x) &&
                (int)std::floor(sb.y) == (int)std::floor(pos.y) &&
                (int)std::floor(sb.z) == (int)std::floor(pos.z)) {
                return true;
            }
        }
        return false;
    };

    const Vec3 offsets[6] = {
        {0, -1, 0}, // DOWN
        {0, 1, 0},  // UP
        {0, 0, -1}, // NORTH
        {0, 0, 1},  // SOUTH
        {-1, 0, 0}, // WEST
        {1, 0, 0}   // EAST
    };

    for (int x = startX; x <= endX; x++) {
        for (int y = startY; y <= endY; y++) {
            for (int z = startZ; z <= endZ; z++) {
                Vec3 c((double)x, (double)y, (double)z);

                // 1. Must not be solid already
                if (isSolid(c)) continue;

                // 2. Must be within place range from player
                if (AutoCrystalMath::distanceToCenter(playerPos, c) > placeRange) continue;

                // 3. Must not intersect player or target
                if (intersectsEntity(playerPos.x, playerPos.y, playerPos.z, x, y, z) ||
                    intersectsEntity(targetPos.x, targetPos.y, targetPos.z, x, y, z)) {
                    continue;
                }

                // 4. Check block space above for the crystal placement (must not be solid)
                Vec3 above1(c.x, c.y + 1.0, c.z);
                if (isSolid(above1)) continue;

                if (!airPlace) {
                    Vec3 above2(c.x, c.y + 2.0, c.z);
                    if (isSolid(above2)) continue;
                }

                // 5. Check solid neighbor to click against (unless airPlace is active)
                bool foundNeighbor = false;
                Vec3 bestNeighbor;
                int bestFace = 1; // Default to UP

                for (const auto& off : offsets) {
                    Vec3 n = c + off;
                    if (isSolid(n)) {
                        foundNeighbor = true;
                        bestNeighbor = n;
                        bestFace = getFaceIndex(n, c);
                        break;
                    }
                }

                if (!foundNeighbor) {
                    if (airPlace) {
                        bestNeighbor = c;
                        bestFace = 1; // UP face of itself
                    } else {
                        continue; // Requires a solid neighbor
                    }
                }

                // 6. Predict crystal explosion damage (placed on top of candidate block)
                Vec3 crystalPos(c.x + 0.5, c.y + 1.0, c.z + 0.5);

                // Check distance to target
                if (predictedTargetPos.distanceTo(crystalPos) > targetRange) continue;

                // Simulate block placement by adding to temporary solid block list
                std::vector<Vec3> tempBlocks = solidBlocks;
                tempBlocks.push_back(c);

                double targetDmg = AutoCrystalMath::calcExplosionDamage(
                    crystalPos, predictedTargetPos, targetHealth, targetAbsorption, targetStats, tempBlocks
                );

                double selfDmg = AutoCrystalMath::calcExplosionDamage(
                    crystalPos, playerPos, playerHealth, playerAbsorption, playerStats, tempBlocks
                );

                // 7. Verify safety and thresholds
                if (targetDmg < minTargetDamage) continue;
                if (selfDmg > maxSelfDamage) continue;

                if (antiSuicide) {
                    if (playerHealth + playerAbsorption - selfDmg < antiSuicideMinHp) continue;
                }

                double score = targetDmg - selfDmg * selfDamageWeight;

                if (score > bestResult.score) {
                    bestResult.score = score;
                    bestResult.blockPos = c;
                    bestResult.neighborPos = bestNeighbor;
                    bestResult.face = bestFace;
                    bestResult.targetDamage = targetDmg;
                    bestResult.selfDamage = selfDmg;
                    bestResult.valid = true;
                }
            }
        }
    }

    return bestResult;
}

} // namespace ravex
