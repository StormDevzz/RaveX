#include "damage_calc.h"
#include <cmath>
#include <algorithm>

static constexpr double TNT_POWER = 4.0;     
static constexpr double TNT_RADIUS = TNT_POWER * 2.0; 

double estimateExposure(
    double tntX, double tntY, double tntZ,
    double targetX, double targetY, double targetZ,
    double distance
) {
    
    
    
    if (distance <= 0.01) return 1.0;
    if (distance >= TNT_RADIUS) return 0.0;

    
    
    double baseFactor = 1.0 - (distance / TNT_RADIUS);

    
    if (distance < 2.0) return std::min(1.0, baseFactor * 1.5);

    return baseFactor;
}

static double applyArmorReduction(double damage, int armorPoints, int armorToughness) {
    
    
    
    double reduction = armorPoints - 4.0 * std::max(damage / 2.0 - armorToughness, armorPoints * 0.2);
    reduction = std::clamp(reduction, armorPoints * 0.2, 20.0);
    double multiplier = std::max(1.0 - reduction / 25.0, 0.04);
    return damage * multiplier;
}

static double applyBlastProtection(double damage, int totalLevel) {
    
    double reduction = std::min(totalLevel * 0.08, 0.80); 
    return damage * (1.0 - reduction);
}

static double applyResistance(double damage, int amplifier) {
    
    double reduction = std::min((amplifier + 1) * 0.2, 1.0);
    return damage * (1.0 - reduction);
}

TntDamageResult calculateTntDamage(const TntDamageConfig& config) {
    TntDamageResult result = {0.0, 0.0, 0.0, false};

    double dx = config.targetX - config.tntX;
    double dy = config.targetY - config.tntY;
    double dz = config.targetZ - config.tntZ;
    double distance = std::sqrt(dx*dx + dy*dy + dz*dz);

    if (distance >= TNT_RADIUS) return result;

    double exposure = estimateExposure(
        config.tntX, config.tntY, config.tntZ,
        config.targetX, config.targetY, config.targetZ,
        distance
    );

    if (exposure <= 0.0) return result;

    
    double impact = (1.0 - distance / TNT_RADIUS) * exposure;
    double rawDamage = std::floor((impact * impact + impact) * 7.0 * TNT_RADIUS + 1.0);
    result.rawDamage = rawDamage;

    
    double finalDmg = rawDamage;

    
    if (config.armorPoints > 0) {
        finalDmg = applyArmorReduction(finalDmg, config.armorPoints, config.armorToughness);
    }

    
    if (config.blastProtLevel > 0) {
        finalDmg = applyBlastProtection(finalDmg, config.blastProtLevel);
    }

    
    if (config.hasResistance) {
        finalDmg = applyResistance(finalDmg, config.resistanceAmplifier);
    }

    result.finalDamage = std::max(finalDmg, 0.0);
    result.lethal = result.finalDamage >= config.targetHealth;

    
    if (result.lethal) {
        result.killProbability = 1.0;
    } else {
        result.killProbability = std::min(result.finalDamage / config.targetHealth, 0.99);
    }

    return result;
}
