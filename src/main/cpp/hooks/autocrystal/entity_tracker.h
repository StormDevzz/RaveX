#pragma once

#include "autocrystal.h"

namespace ravex {

class EntityTracker {
public:
    
    
    static Vec3 predictPosition(const Vec3& currentPos, double motionX, double motionY, double motionZ, double ticksAhead);

    
    static double getEffectiveHealth(double health, double absorption);
};

} 
