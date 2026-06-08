#pragma once

#include "autocrystal.h"

namespace ravex {

class EntityTracker {
public:
    // Предсказание будущей позиции сущности на основе её текущей позиции и вектора движения
    // ticksAhead: количество тиков для экстраполяции (например, 1.0 или 2.0 тика)
    static Vec3 predictPosition(const Vec3& currentPos, double motionX, double motionY, double motionZ, double ticksAhead);

    // Расчёт эффективного здоровья сущности (HP + Absorption)
    static double getEffectiveHealth(double health, double absorption);
};

} // namespace ravex
