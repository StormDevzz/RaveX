#pragma once

#include "autocrystal.hpp"

namespace ravex {

class ArmorCalc {
public:

    static double applyArmorMitigation(double rawDamage, double armorValue, double toughness);


    static bool hasCriticalArmorDurability(const EntityStats& stats, double minThresholdPercent);


    static double getMinArmorDurability(const EntityStats& stats);
};

}
