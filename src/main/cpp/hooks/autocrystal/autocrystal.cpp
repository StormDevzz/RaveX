#include "autocrystal.h"
#include "damage.h"
#include "armor.h"
#include "effects.h"
#include "entity_tracker.h"
#include "conditions.h"

#include <cmath>
#include <vector>
#include <algorithm>
#include <sstream>
#include <limits>

namespace ravex {

double AutoCrystalMath::calcExplosionDamage(
    const Vec3& explosionPos,
    const Vec3& entityPos,
    double      entityHealth,
    double      entityAbsorption,
    const EntityStats& stats,
    const std::vector<Vec3>& blocks)
{
    double rawDmg = DamageCalc::calcRawExplosionDamage(explosionPos, entityPos, blocks);
    double finalDmg = EffectsCalc::getFinalDamage(rawDmg, stats);
    return finalDmg;
}

bool AutoCrystalMath::isValidBase(const Vec3& pos, const std::vector<Vec3>& blocks) {
    for (const Vec3& b : blocks) {
        if ((int)b.x == (int)pos.x &&
            (int)b.y == (int)pos.y &&
            (int)b.z == (int)pos.z) {
            return true;
        }
    }
    return false;
}

bool AutoCrystalMath::isOccupied(const Vec3& crystalPos, const std::vector<CrystalEntity>& crystals) {
    for (const CrystalEntity& c : crystals) {
        double dx = std::abs(c.pos.x - crystalPos.x);
        double dz = std::abs(c.pos.z - crystalPos.z);
        double dy = std::abs(c.pos.y - crystalPos.y);
        if (dx < 1.0 && dz < 1.0 && dy < 2.0) return true;
    }
    return false;
}

double AutoCrystalMath::distanceToCenter(const Vec3& playerPos, const Vec3& blockPos) {
    Vec3 center = {blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5};
    Vec3 eyePos = {playerPos.x, playerPos.y + 1.62, playerPos.z};
    return eyePos.distanceTo(center);
}

CrystalPlacement AutoCrystalMath::findBestPlacement(
    const Vec3&               playerPos,
    double                    playerHealth,
    double                    playerAbsorption,
    const EntityStats&        playerStats,
    const Vec3&               targetPos,
    double                    targetHealth,
    double                    targetAbsorption,
    const EntityStats&        targetStats,
    const std::vector<Vec3>&  blocks,
    const AutoCrystalConfig&  config)
{
    CrystalPlacement best;
    best.valid = false;
    best.score = -std::numeric_limits<double>::infinity();

    // Экстраполируем будущую позицию цели на основе настраиваемого predictTicks
    Vec3 predictedTargetPos = EntityTracker::predictPosition(
        targetPos, targetStats.motionX, targetStats.motionY, targetStats.motionZ, config.predictTicks
    );

    // Проверяем, находится ли игрок в безопасной яме (Hole)
    bool playerInHole = ConditionValidator::isInHole(playerPos, blocks);

    // Цель неуязвима под эффектом Resistance V
    if (ConditionValidator::isTargetInvulnerable(targetStats)) {
        return best;
    }

    double targetEffHp = EntityTracker::getEffectiveHealth(targetHealth, targetAbsorption);

    for (const Vec3& block : blocks) {
        Vec3 crystalPos = {block.x + 0.5, block.y + 1.0, block.z + 0.5};

        double dist = distanceToCenter(playerPos, block);
        if (dist > config.placeRange) continue;

        // Проверяем свободное пространство над обсидианом/бедроком
        bool spaceBlocked = false;
        Vec3 above1 = {block.x, block.y + 1.0, block.z};
        Vec3 above2 = {block.x, block.y + 2.0, block.z};
        for (const Vec3& b : blocks) {
            if ((int)b.x == (int)above1.x && (int)b.y == (int)above1.y && (int)b.z == (int)above1.z) {
                spaceBlocked = true; break;
            }
            if ((int)b.x == (int)above2.x && (int)b.y == (int)above2.y && (int)b.z == (int)above2.z) {
                spaceBlocked = true; break;
            }
        }
        if (spaceBlocked) continue;

        // Рассчитываем урон по цели (по предсказанным координатам) и себе
        double targetDmg = calcExplosionDamage(crystalPos, predictedTargetPos, targetHealth, targetAbsorption, targetStats, blocks);
        double selfDmg   = calcExplosionDamage(crystalPos, playerPos, playerHealth, playerAbsorption, playerStats, blocks);

        // Строгая проверка безопасности для игрока
        if (!ConditionValidator::isPlacementSafe(playerHealth, playerAbsorption, selfDmg, playerStats, config, playerInHole)) {
            continue;
        }

        // Если у цели критическое состояние брони, опускаем требования к минимальному урону до 1.0 урона
        double requiredMinDmg = config.minTargetDamage;
        if (ConditionValidator::shouldForceArmorBreak(targetStats, targetEffHp, config.armorBreaker, config.armorPercent)) {
            requiredMinDmg = std::min(1.0, config.minTargetDamage);
        }

        if (targetDmg < requiredMinDmg) continue;

        // Бонус за убийство цели
        double deathBonus = 0.0;
        if (targetEffHp - targetDmg <= 0.0) {
            deathBonus = 20.0;
        }

        // Бонус за снятие (поп) тотема цели (+30 очков для приоритета)
        double totemPopBonus = 0.0;
        if (ConditionValidator::shouldForcePopTotem(targetEffHp, targetDmg, targetStats.totemCount, config.totemDetection)) {
            totemPopBonus = 30.0;
        }

        // Бонус за малое здоровье цели
        double healthBonus = 0.0;
        if (targetHealth < 8.0) {
            healthBonus = config.targetHealthBonus * (8.0 - targetHealth);
        }

        double score = targetDmg - selfDmg * config.selfDamageWeight + deathBonus + totemPopBonus + healthBonus;

        if (score > best.score) {
            best.score        = score;
            best.blockPos     = block;
            best.crystalPos   = crystalPos;
            best.targetDamage = targetDmg;
            best.selfDamage   = selfDmg;
            best.valid        = true;
        }
    }

    return best;
}

bool AutoCrystalMath::findBestBreak(
    const std::vector<CrystalEntity>& crystals,
    const Vec3&  playerPos,
    double       playerHealth,
    double       playerAbsorption,
    const EntityStats& playerStats,
    const Vec3&  targetPos,
    double       targetHealth,
    double       targetAbsorption,
    const EntityStats& targetStats,
    const std::vector<Vec3>& blocks,
    const AutoCrystalConfig& config,
    int&         outEntityId,
    Vec3&        outPos,
    double&      outDamage)
{
    double bestScore = -std::numeric_limits<double>::infinity();
    bool found = false;

    Vec3 eyePos = {playerPos.x, playerPos.y + 1.62, playerPos.z};

    Vec3 predictedTargetPos = EntityTracker::predictPosition(
        targetPos, targetStats.motionX, targetStats.motionY, targetStats.motionZ, config.predictTicks
    );

    bool playerInHole = ConditionValidator::isInHole(playerPos, blocks);

    if (ConditionValidator::isTargetInvulnerable(targetStats)) {
        return false;
    }

    double targetEffHp = EntityTracker::getEffectiveHealth(targetHealth, targetAbsorption);

    for (const CrystalEntity& crystal : crystals) {
        double dist = eyePos.distanceTo(crystal.pos);
        if (dist > config.breakRange) continue;

        double targetDmg = calcExplosionDamage(crystal.pos, predictedTargetPos, targetHealth, targetAbsorption, targetStats, blocks);
        double selfDmg   = calcExplosionDamage(crystal.pos, playerPos, playerHealth, playerAbsorption, playerStats, blocks);

        // Строгая проверка безопасности для игрока
        if (!ConditionValidator::isPlacementSafe(playerHealth, playerAbsorption, selfDmg, playerStats, config, playerInHole)) {
            continue;
        }

        double requiredMinDmg = config.minTargetDamage;
        if (ConditionValidator::shouldForceArmorBreak(targetStats, targetEffHp, config.armorBreaker, config.armorPercent)) {
            requiredMinDmg = std::min(1.0, config.minTargetDamage);
        }

        if (targetDmg < requiredMinDmg) continue;

        double deathBonus = (targetEffHp - targetDmg <= 0.0) ? 20.0 : 0.0;
        
        double totemPopBonus = 0.0;
        if (ConditionValidator::shouldForcePopTotem(targetEffHp, targetDmg, targetStats.totemCount, config.totemDetection)) {
            totemPopBonus = 30.0;
        }

        double score = targetDmg - selfDmg * config.selfDamageWeight + deathBonus + totemPopBonus;

        if (score > bestScore) {
            bestScore    = score;
            outEntityId  = crystal.entityId;
            outPos       = crystal.pos;
            outDamage    = targetDmg;
            found        = true;
        }
    }

    return found;
}

AutoCrystalResult AutoCrystalMath::tick(
    const Vec3&                   playerPos,
    double                        playerHealth,
    double                        playerAbsorption,
    const EntityStats&            playerStats,
    const Vec3&                   targetPos,
    double                        targetHealth,
    double                        targetAbsorption,
    const EntityStats&            targetStats,
    const std::vector<Vec3>&      blocks,
    const std::vector<CrystalEntity>& activeCrystals,
    const AutoCrystalConfig&      config)
{
    AutoCrystalResult result;
    result.shouldPlace = false;
    result.shouldBreak = false;
    result.breakEntityId = -1;
    result.breakDamage = 0.0;

    std::ostringstream dbg;

    // Фаза 1: Размещение
    CrystalPlacement placement = findBestPlacement(
        playerPos, playerHealth, playerAbsorption, playerStats,
        targetPos, targetHealth, targetAbsorption, targetStats,
        blocks, config
    );

    if (placement.valid) {
        if (!isOccupied(placement.crystalPos, activeCrystals)) {
            result.shouldPlace    = true;
            result.bestPlacement  = placement;
            dbg << "PLACE at (" << placement.blockPos.x << "," << placement.blockPos.y
                << "," << placement.blockPos.z << ") "
                << "tDmg=" << placement.targetDamage
                << " sDmg=" << placement.selfDamage
                << " score=" << placement.score << "; ";
        } else {
            dbg << "PLACE occupied; ";
        }
    } else {
        dbg << "NO placement; ";
    }

    // Фаза 2: Подрыв
    int    breakId  = -1;
    Vec3   breakPos;
    double breakDmg = 0.0;

    bool foundBreak = findBestBreak(
        activeCrystals,
        playerPos, playerHealth, playerAbsorption, playerStats,
        targetPos, targetHealth, targetAbsorption, targetStats,
        blocks, config,
        breakId, breakPos, breakDmg
    );

    if (foundBreak) {
        result.shouldBreak   = true;
        result.breakEntityId = breakId;
        result.breakPos      = breakPos;
        result.breakDamage   = breakDmg;
        dbg << "BREAK id=" << breakId << " tDmg=" << breakDmg;
    } else {
        dbg << "NO break";
    }

    result.debugInfo = dbg.str();
    return result;
}

} // namespace ravex
