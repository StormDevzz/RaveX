#include "breaker.hpp"
#include <limits>

namespace ravex {

BreakResult findBestBreakBlock(
    const Vec3& playerPos, double playerHp, double playerAbs, const EntityStats& playerStats,
    const Vec3& targetPos, double targetHp, double targetAbs, const EntityStats& targetStats,
    const std::vector<Vec3>& solidBlocks,
    const std::vector<Vec3>& candidates,
    double breakRange, double crystalPlaceRange,
    double minTargetDamage, double maxSelfDamage, double selfDamageWeight,
    bool antiSuicide, double antiSuicideMinHp
) {
    BreakResult best;
    best.valid = false;
    best.score = -std::numeric_limits<double>::infinity();

    AutoCrystalConfig config;
    config.placeRange = crystalPlaceRange;
    config.placeWallRange = crystalPlaceRange;
    config.minTargetDamage = minTargetDamage;
    config.maxSelfDamage = maxSelfDamage;
    config.selfDamageWeight = selfDamageWeight;
    config.antiSuicide = antiSuicide;
    
    for (const auto& cand : candidates) {
        if (AutoCrystalMath::distanceToCenter(playerPos, cand) > breakRange) continue;

        
        std::vector<Vec3> tempBlocks;
        for (const auto& sb : solidBlocks) {
            if ((int)sb.x == (int)cand.x && (int)sb.y == (int)cand.y && (int)sb.z == (int)cand.z) continue;
            tempBlocks.push_back(sb);
        }

        
        CrystalPlacement placement = AutoCrystalMath::findBestPlacement(
            playerPos, playerHp, playerAbs, playerStats,
            targetPos, targetHp, targetAbs, targetStats,
            tempBlocks, config
        );

        if (placement.valid) {
            double score = placement.targetDamage - placement.selfDamage * selfDamageWeight;
            
            
            int cx = (int)std::floor(cand.x);
            int cy = (int)std::floor(cand.y);
            int cz = (int)std::floor(cand.z);
            int tx = (int)std::floor(targetPos.x);
            int ty = (int)std::floor(targetPos.y);
            int tz = (int)std::floor(targetPos.z);
            
            bool isSurroundOrSupport = false;
            if (cx == tx && cy == ty - 1 && cz == tz) {
                isSurroundOrSupport = true;
            } else if (cy == ty || cy == ty + 1) {
                int dx = std::abs(cx - tx);
                int dz = std::abs(cz - tz);
                if (dx <= 1 && dz <= 1 && (dx + dz > 0)) {
                    isSurroundOrSupport = true;
                }
            } else if (cx == tx && cy == ty + 2 && cz == tz) {
                isSurroundOrSupport = true;
            }
            
            if (isSurroundOrSupport) {
                score += 50.0;
            }
            
            if (antiSuicide) {
                if (playerHp + playerAbs - placement.selfDamage < antiSuicideMinHp) continue;
            }

            if (score > best.score) {
                best.score = score;
                best.breakBlock = cand;
                best.crystalPos = placement.crystalPos;
                best.crystalBlockPos = placement.blockPos;
                best.targetDamage = placement.targetDamage;
                best.selfDamage = placement.selfDamage;
                best.valid = true;
            }
        }
    }

    return best;
}

} 
