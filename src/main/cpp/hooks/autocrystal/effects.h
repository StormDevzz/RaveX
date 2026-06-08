#pragma once

#include "autocrystal.h"

namespace ravex {

class EffectsCalc {
public:
    // Применение зелья Сопротивления (Resistance)
    // Уровень 1 = 20% поглощения, уровень 5 = 100% (иммунитет)
    static double applyResistancePotion(double damage, double resistanceLevel);

    // Применение EPF (Enchantment Protection Factor) для взрывов
    // Каждый пункт EPF дает 4% защиты, макс 80% (20 EPF)
    static double applyExplosionEnchantments(double damage, double blastProtEpf, double protEpf);

    // Полный расчёт снижения урона: броня -> эффекты -> чары
    static double getFinalDamage(double rawDamage, const EntityStats& stats);

    // Проверка на абсолютный иммунитет от Сопротивления (Resistance V+)
    static bool isImmune(const EntityStats& stats);
};

} // namespace ravex
