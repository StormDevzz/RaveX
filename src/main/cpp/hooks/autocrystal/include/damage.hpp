#pragma once

#include "autocrystal.hpp"
#include <vector>

namespace ravex {

class DamageCalc {
public:

    static double calcExposure(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks);


    static double calcRawExplosionDamage(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks);
};

}
