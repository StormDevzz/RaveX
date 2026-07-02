#pragma once

#include "autocrystal.h"

namespace ravex {

class EffectsCalc {
public:
    
    
    static double applyResistancePotion(double damage, double resistanceLevel);

    
    
    static double applyExplosionEnchantments(double damage, double blastProtEpf, double protEpf);

    
    static double getFinalDamage(double rawDamage, const EntityStats& stats);

    
    static bool isImmune(const EntityStats& stats);
};

} 

namespace ravex {
namespace effects {
class VisualEffects {
public:
    VisualEffects();
    void spawn();
};
}
}
