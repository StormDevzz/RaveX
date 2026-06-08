#include "effects.h"
#include "armor.h"
#include <algorithm>

namespace ravex {

double EffectsCalc::applyResistancePotion(double damage, double resistanceLevel) {
    if (resistanceLevel <= 0.0) return damage;
    if (resistanceLevel >= 5.0) return 0.0; // Resistance V дает 100% защиту в ваниле
    
    double reduction = resistanceLevel * 0.20;
    double finalDmg = damage * (1.0 - reduction);
    return std::max(0.0, finalDmg);
}

double EffectsCalc::applyExplosionEnchantments(double damage, double blastProtEpf, double protEpf) {
    // Взрывы снижаются как Blast Protection, так и обычной Protection
    double totalEpf = blastProtEpf + protEpf;
    totalEpf = std::min(20.0, totalEpf);
    
    double reduction = totalEpf * 0.04; // 4% за каждый уровень EPF (до 80% максимум)
    double finalDmg = damage * (1.0 - reduction);
    return std::max(0.0, finalDmg);
}

double EffectsCalc::getFinalDamage(double rawDamage, const EntityStats& stats) {
    if (rawDamage <= 0.0) return 0.0;
    
    // Порядок применения защиты в Minecraft:
    // 1. Броня (Armor)
    double dmgAfterArmor = ArmorCalc::applyArmorMitigation(rawDamage, stats.armorValue, stats.toughness);
    
    // 2. Эффект Сопротивления (Resistance)
    double dmgAfterResist = applyResistancePotion(dmgAfterArmor, stats.resistanceLevel);
    
    // 3. Зачарования (EPF)
    double finalDmg = applyExplosionEnchantments(dmgAfterResist, stats.blastProtectionEpf, stats.protectionEpf);
    
    return finalDmg;
}

bool EffectsCalc::isImmune(const EntityStats& stats) {
    return stats.resistanceLevel >= 5.0;
}

} // namespace ravex
