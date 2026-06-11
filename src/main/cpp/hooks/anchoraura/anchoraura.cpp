#include "anchoraura.h"
#include "../autocrystal/entity_tracker.h"
#include "../autocrystal/effects.h"
#include "../autocrystal/damage.h"
#include <cmath>
#include <algorithm>
#include <limits>

namespace ravex {

static constexpr double ANCHOR_EXPLOSION_POWER = 5.0;

static bool intersectsEntity(double ex, double ey, double ez, int bx, int by, int bz) {
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

// Custom Respawn Anchor raw damage calculator with power 5.0
static double calcRawAnchorDamage(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks) {
    Vec3 center = {entityPos.x, entityPos.y + 0.9, entityPos.z};
    double distance = explosionPos.distanceTo(center);
    double maxDistance = ANCHOR_EXPLOSION_POWER * 2.0;

    if (distance > maxDistance) return 0.0;

    double exposure = DamageCalc::calcExposure(explosionPos, entityPos, blocks);
    double impact = (1.0 - distance / maxDistance) * exposure;

    double baseDamage = (impact * impact + impact) / 2.0 * 7.0 * maxDistance + 1.0;
    return baseDamage;
}

static double calcAnchorDamage(
    const Vec3& explosionPos,
    const Vec3& entityPos,
    double      entityHealth,
    double      entityAbsorption,
    const EntityStats& stats,
    const std::vector<Vec3>& blocks
) {
    double rawDmg = calcRawAnchorDamage(explosionPos, entityPos, blocks);
    double finalDmg = EffectsCalc::getFinalDamage(rawDmg, stats);
    return finalDmg;
}

AnchorAuraResult AnchorAuraMath::findBestAnchorPlace(
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
    bool                      alwaysConsiderDurability,
    double                    armorDurabilityThreshold
) {
    AnchorAuraResult bestResult;
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

    // Durability check
    bool armorWeak = false;
    if (alwaysConsiderDurability) {
        if (targetStats.helmetDurability > 0 && targetStats.helmetDurability < armorDurabilityThreshold) armorWeak = true;
        if (targetStats.chestplateDurability > 0 && targetStats.chestplateDurability < armorDurabilityThreshold) armorWeak = true;
        if (targetStats.leggingsDurability > 0 && targetStats.leggingsDurability < armorDurabilityThreshold) armorWeak = true;
        if (targetStats.bootsDurability > 0 && targetStats.bootsDurability < armorDurabilityThreshold) armorWeak = true;
    }

    for (int x = startX; x <= endX; x++) {
        for (int y = startY; y <= endY; y++) {
            for (int z = startZ; z <= endZ; z++) {
                Vec3 c((double)x, (double)y, (double)z);

                if (isSolid(c)) continue;

                if (AutoCrystalMath::distanceToCenter(playerPos, c) > placeRange) continue;

                if (intersectsEntity(playerPos.x, playerPos.y, playerPos.z, x, y, z) ||
                    intersectsEntity(targetPos.x, targetPos.y, targetPos.z, x, y, z)) {
                    continue;
                }

                // Check block above for the anchor placement (must not be solid)
                Vec3 above(c.x, c.y + 1.0, c.z);
                if (isSolid(above)) continue;

                // Find solid neighbor to click against
                bool foundNeighbor = false;
                Vec3 bestNeighbor;
                int bestFace = 1;

                for (const auto& off : offsets) {
                    Vec3 n = c + off;
                    if (isSolid(n)) {
                        foundNeighbor = true;
                        bestNeighbor = n;
                        bestFace = getFaceIndex(n, c);
                        break;
                    }
                }

                if (!foundNeighbor) continue;

                // Explosion coordinates (center of the anchor block)
                Vec3 anchorExplosionPos(c.x + 0.5, c.y + 0.5, c.z + 0.5);

                if (predictedTargetPos.distanceTo(anchorExplosionPos) > targetRange) continue;

                // Simulate block placement
                std::vector<Vec3> tempBlocks = solidBlocks;
                tempBlocks.push_back(c);

                double targetDmg = calcAnchorDamage(
                    anchorExplosionPos, predictedTargetPos, targetHealth, targetAbsorption, targetStats, tempBlocks
                );

                double selfDmg = calcAnchorDamage(
                    anchorExplosionPos, playerPos, playerHealth, playerAbsorption, playerStats, tempBlocks
                );

                if (targetDmg < minTargetDamage) continue;
                if (selfDmg > maxSelfDamage) continue;

                if (antiSuicide) {
                    if (playerHealth + playerAbsorption - selfDmg < antiSuicideMinHp) continue;
                }

                double score = targetDmg - selfDmg * selfDamageWeight;

                // Apply score boost if target has low armor durability to speed up armor breaking
                if (armorWeak) {
                    score += 10.0;
                }

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
