#pragma once
#include "autocrystal.h"
#include <vector>

namespace ravex {

struct BreakResult {
    bool valid;
    Vec3 breakBlock;
    Vec3 crystalPos;
    Vec3 crystalBlockPos;
    double targetDamage;
    double selfDamage;
    double score;
};

BreakResult findBestBreakBlock(
    const Vec3& playerPos, double playerHp, double playerAbs, const EntityStats& playerStats,
    const Vec3& targetPos, double targetHp, double targetAbs, const EntityStats& targetStats,
    const std::vector<Vec3>& solidBlocks,
    const std::vector<Vec3>& candidates,
    double breakRange, double crystalPlaceRange,
    double minTargetDamage, double maxSelfDamage, double selfDamageWeight,
    bool antiSuicide, double antiSuicideMinHp
);

} // namespace ravex
