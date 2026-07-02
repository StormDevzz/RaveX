#include "effects.hpp"
#include "armor.hpp"
#include <algorithm>

namespace ravex {

double EffectsCalc::applyResistancePotion(double damage, double resistanceLevel) {
    if (resistanceLevel <= 0.0) return damage;
    if (resistanceLevel >= 5.0) return 0.0; 
    
    double reduction = resistanceLevel * 0.20;
    double finalDmg = damage * (1.0 - reduction);
    return std::max(0.0, finalDmg);
}

double EffectsCalc::applyExplosionEnchantments(double damage, double blastProtEpf, double protEpf) {
    
    double totalEpf = blastProtEpf + protEpf;
    totalEpf = std::min(20.0, totalEpf);
    
    double reduction = totalEpf * 0.04; 
    double finalDmg = damage * (1.0 - reduction);
    return std::max(0.0, finalDmg);
}

double EffectsCalc::getFinalDamage(double rawDamage, const EntityStats& stats) {
    if (rawDamage <= 0.0) return 0.0;
    
    
    
    double dmgAfterArmor = ArmorCalc::applyArmorMitigation(rawDamage, stats.armorValue, stats.toughness);
    
    
    double dmgAfterResist = applyResistancePotion(dmgAfterArmor, stats.resistanceLevel);
    
    
    double finalDmg = applyExplosionEnchantments(dmgAfterResist, stats.blastProtectionEpf, stats.protectionEpf);
    
    return finalDmg;
}

bool EffectsCalc::isImmune(const EntityStats& stats) {
    return stats.resistanceLevel >= 5.0;
}

} 
