#pragma once
#include "../autocrystal/autocrystal.h"
#include <vector>

namespace ravex {

struct BasePlaceResult {
    bool   valid;
    Vec3   blockPos;       // Where obsidian block should be placed
    Vec3   neighborPos;    // Adjacent solid block to click against
    int    face;           // Ordinal of the clicked block face (DOWN=0, UP=1, NORTH=2, SOUTH=3, WEST=4, EAST=5)
    double score;
    double targetDamage;
    double selfDamage;
};

class BasePlaceMath {
public:
    static BasePlaceResult findBestBasePlace(
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
    );
};

} // namespace ravex
