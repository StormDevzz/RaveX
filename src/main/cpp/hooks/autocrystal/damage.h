#pragma once

#include "autocrystal.h"
#include <vector>

namespace ravex {

class DamageCalc {
public:
    // Расчёт exposure (какая доля лучей проходит без преград)
    static double calcExposure(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks);

    // Базовый (чистый) урон от взрыва до применения брони и эффектов
    static double calcRawExplosionDamage(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks);
};

} // namespace ravex
