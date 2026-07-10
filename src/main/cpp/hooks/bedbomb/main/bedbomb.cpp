#include "bedbomb.hpp"
#include <cmath>
#include <algorithm>
#include <limits>

namespace ravex {

static constexpr double BED_EXPLOSION_POWER = 5.0;

double calcBedDamage(double dist, double explosionPower) {
    if (dist > 10.0) return 0.0;
    double exposure = 1.0;
    double rawDamage = (explosionPower * 2.0 + 0.0) * sqrt((explosionPower * 2.0 + 0.0) - dist) / (explosionPower * 2.0 + 0.0) * exposure * 3.0;
    return std::max(0.0, rawDamage);
}

Vec3 findBestBedPlace(Vec3 playerPos, Vec3 enemyPos, double range) {
    Vec3 dir = enemyPos - playerPos;
    double len = dir.length();
    if (len < 0.01) return {std::numeric_limits<double>::max(), 0, 0};

    dir = dir * (1.0 / len);
    Vec3 placePos = playerPos + dir * std::min(range, len * 0.7);

    placePos.x = round(placePos.x - 0.5) + 0.5;
    placePos.z = round(placePos.z - 0.5) + 0.5;
    placePos.y = round(placePos.y);

    return placePos;
}

}
