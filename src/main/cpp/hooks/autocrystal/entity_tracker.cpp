#include "entity_tracker.h"
#include <cmath>

namespace ravex {

Vec3 EntityTracker::predictPosition(const Vec3& currentPos, double motionX, double motionY, double motionZ, double ticksAhead) {
    // В Minecraft движение игрока в воздухе или по земле имеет инерцию/трение.
    // Средний коэффициент трения/сопротивления воздуха составляет 0.91 для LivingEntity,
    // а гравитационное ускорение — 0.08 блока за тик.
    double friction = 0.91;
    double gravity = 0.08;
    
    double predX = currentPos.x;
    double predY = currentPos.y;
    double predZ = currentPos.z;
    
    double vx = motionX;
    double vy = motionY;
    double vz = motionZ;
    
    int fullTicks = static_cast<int>(std::floor(ticksAhead));
    for (int i = 0; i < fullTicks; i++) {
        predX += vx;
        predY += vy;
        predZ += vz;
        
        vx *= friction;
        vy = (vy - gravity) * friction;
        vz *= friction;
    }
    
    // Экстраполяция остаточной дробной части тика
    double frac = ticksAhead - fullTicks;
    if (frac > 0.0) {
        predX += vx * frac;
        predY += vy * frac;
        predZ += vz * frac;
    }
    
    return {predX, predY, predZ};
}

double EntityTracker::getEffectiveHealth(double health, double absorption) {
    return health + absorption;
}

} // namespace ravex
