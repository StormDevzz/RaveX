#pragma once

#include "autocrystal.hpp"
#include <vector>

namespace ravex {

class ConditionValidator {
public:

    static bool isTargetInvulnerable(const EntityStats& targetStats);


    static bool isInHole(const Vec3& pos, const std::vector<Vec3>& blocks);


    static bool isPlacementSafe(
        double playerHealth,
        double playerAbsorption,
        double selfDamage,
        const EntityStats& playerStats,
        const AutoCrystalConfig& config,
        bool playerInHole,
        bool isBreakPhase
    );


    static bool shouldForceArmorBreak(const EntityStats& targetStats, double targetEffectiveHp, bool armorBreaker, double armorPercent);


    static bool shouldForcePopTotem(double targetEffectiveHp, double targetDmg, double targetTotems, bool totemDetection);
};

}

namespace ravex {
namespace conditions {
class GrimConditions {
public:
    GrimConditions();
    bool check();
};
}
}
