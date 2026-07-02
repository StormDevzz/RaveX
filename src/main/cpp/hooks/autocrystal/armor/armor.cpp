#include "armor.hpp"
#include <algorithm>

namespace ravex {

double ArmorCalc::applyArmorMitigation(double rawDamage, double armorValue, double toughness) {
    double defensePoints = armorValue;
    double armorToughness = toughness;
    
    
    double denominator = 2.0 + armorToughness / 4.0;
    double term1 = defensePoints / 5.0;
    double term2 = defensePoints - rawDamage / denominator;
    
    double maxArmorReduction = std::max(term1, term2);
    double cappedReduction = std::min(20.0, maxArmorReduction);
    
    double finalDamage = rawDamage * (1.0 - cappedReduction / 25.0);
    return std::max(0.0, finalDamage);
}

bool ArmorCalc::hasCriticalArmorDurability(const EntityStats& stats, double minThresholdPercent) {
    
    if (stats.helmetDurability > 0.0 && stats.helmetDurability <= minThresholdPercent) return true;
    if (stats.chestplateDurability > 0.0 && stats.chestplateDurability <= minThresholdPercent) return true;
    if (stats.leggingsDurability > 0.0 && stats.leggingsDurability <= minThresholdPercent) return true;
    if (stats.bootsDurability > 0.0 && stats.bootsDurability <= minThresholdPercent) return true;
    return false;
}

double ArmorCalc::getMinArmorDurability(const EntityStats& stats) {
    double minDur = 100.0;
    bool hasArmor = false;
    
    if (stats.helmetDurability > 0.0) {
        minDur = std::min(minDur, stats.helmetDurability);
        hasArmor = true;
    }
    if (stats.chestplateDurability > 0.0) {
        minDur = std::min(minDur, stats.chestplateDurability);
        hasArmor = true;
    }
    if (stats.leggingsDurability > 0.0) {
        minDur = std::min(minDur, stats.leggingsDurability);
        hasArmor = true;
    }
    if (stats.bootsDurability > 0.0) {
        minDur = std::min(minDur, stats.bootsDurability);
        hasArmor = true;
    }
    
    return hasArmor ? minDur : 0.0;
}

} 
