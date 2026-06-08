#pragma once

#include "autocrystal.h"

namespace ravex {

class ArmorCalc {
public:
    // Применение базового снижения урона броней по ванильной формуле
    static double applyArmorMitigation(double rawDamage, double armorValue, double toughness);

    // Проверка, есть ли детали брони с критически низкой прочностью
    static bool hasCriticalArmorDurability(const EntityStats& stats, double minThresholdPercent);

    // Получить наименьшую прочность среди деталей брони
    static double getMinArmorDurability(const EntityStats& stats);
};

} // namespace ravex
