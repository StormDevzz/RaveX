#include "conditions.hpp"
#include "armor.hpp"
#include "effects.hpp"
#include <cmath>
#include <algorithm>

namespace ravex {

static bool hasBlockAt(int x, int y, int z, const std::vector<Vec3>& blocks) {
    for (const auto& b : blocks) {
        if ((int)std::floor(b.x) == x &&
            (int)std::floor(b.y) == y &&
            (int)std::floor(b.z) == z) {
            return true;
        }
    }
    return false;
}

bool ConditionValidator::isTargetInvulnerable(const EntityStats& targetStats) {
    return EffectsCalc::isImmune(targetStats);
}

bool ConditionValidator::isInHole(const Vec3& pos, const std::vector<Vec3>& blocks) {
    int px = (int)std::floor(pos.x);
    int py = (int)std::floor(pos.y);
    int pz = (int)std::floor(pos.z);
    
    return hasBlockAt(px - 1, py, pz, blocks) &&
           hasBlockAt(px + 1, py, pz, blocks) &&
           hasBlockAt(px, py, pz - 1, blocks) &&
           hasBlockAt(px, py, pz + 1, blocks) &&
           hasBlockAt(px, py - 1, pz, blocks);
}

bool ConditionValidator::isPlacementSafe(
    double playerHealth,
    double playerAbsorption,
    double selfDamage,
    const EntityStats& playerStats,
    const AutoCrystalConfig& config,
    bool playerInHole,
    bool isBreakPhase)
{
    if (config.suicide) {
        return true;
    }

    if (config.antiSuicide) {
        bool checkSuicide = true;
        if (isBreakPhase && !config.antiSuicideCheckBreaking) {
            checkSuicide = false;
        }
        if (playerStats.totemCount > 0.0 && config.antiSuicideIgnoreWithTotem) {
            checkSuicide = false;
        }

        if (checkSuicide) {
            double netHealth = playerHealth + playerAbsorption - selfDamage;
            if (netHealth <= 0.5) return false;
        }
    }

    double allowedSelfDmg = playerInHole ? (config.maxSelfDamage * 1.5) : config.maxSelfDamage;
    if (selfDamage > allowedSelfDmg) {
        return false;
    }

    double minDur = ArmorCalc::getMinArmorDurability(playerStats);
    if (minDur > 0.0 && minDur <= 5.0 && selfDamage > 1.5) {
        return false;
    }

    return true;
}

bool ConditionValidator::shouldForceArmorBreak(const EntityStats& targetStats, double targetEffectiveHp, bool armorBreaker, double armorPercent) {
    if (!armorBreaker) return false;
    
    
    
    double minDur = ArmorCalc::getMinArmorDurability(targetStats);
    if (minDur > 0.0 && minDur <= armorPercent && targetEffectiveHp <= 28.0) {
        return true;
    }
    return false;
}

bool ConditionValidator::shouldForcePopTotem(double targetEffectiveHp, double targetDmg, double targetTotems, bool totemDetection) {
    if (!totemDetection) return false;
    
    
    
    if (targetTotems > 0.0 && targetDmg >= targetEffectiveHp) {
        return true;
    }
    return false;
}

} 
