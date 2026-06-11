#pragma once
#include "../autocrystal/autocrystal.h"
#include <vector>

namespace ravex {

struct AnchorAuraResult {
    bool   valid;
    Vec3   blockPos;       // Where to place or target the Respawn Anchor
    Vec3   neighborPos;    // Adjacent block to place against
    int    face;           // clicked block face ordinal
    double score;
    double targetDamage;
    double selfDamage;
};

class AnchorAuraMath {
public:
    static AnchorAuraResult findBestAnchorPlace(
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
    );
};

} // namespace ravex
