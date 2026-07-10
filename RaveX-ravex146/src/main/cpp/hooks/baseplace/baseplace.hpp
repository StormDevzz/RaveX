#pragma once
#include "../autocrystal/include/autocrystal.hpp"
#include <vector>

namespace ravex {

struct BasePlaceResult {
    bool   valid;
    Vec3   blockPos;
    Vec3   neighborPos;
    int    face;
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

}
