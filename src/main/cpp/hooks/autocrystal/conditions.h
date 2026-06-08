#pragma once

#include "autocrystal.h"
#include <vector>

namespace ravex {

class ConditionValidator {
public:
    // Проверка на абсолютную неуязвимость цели (например, Resistance V)
    static bool isTargetInvulnerable(const EntityStats& targetStats);

    // Проверяет, находится ли игрок в safe-яме (Hole) (обсидиан/бедрок со всех сторон)
    static bool isInHole(const Vec3& pos, const std::vector<Vec3>& blocks);

    // Строгая проверка безопасности для игрока (самоурон, прочность брони)
    static bool isPlacementSafe(
        double playerHealth,
        double playerAbsorption,
        double selfDamage,
        const EntityStats& playerStats,
        const AutoCrystalConfig& config,
        bool playerInHole
    );

    // Стоит ли снизить требования к минимальному урону, чтобы сломать броню цели
    static bool shouldForceArmorBreak(const EntityStats& targetStats, double targetEffectiveHp, bool armorBreaker, double armorPercent);

    // Стоит ли проигнорировать стандартные ограничения урона/безопасности ради снятия (pop) тотема цели
    static bool shouldForcePopTotem(double targetEffectiveHp, double targetDmg, double targetTotems, bool totemDetection);
};

} // namespace ravex
