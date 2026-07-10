#include "damage.hpp"
#include <cmath>
#include <algorithm>

namespace ravex {

static constexpr double CRYSTAL_EXPLOSION_POWER = 6.0;

double DamageCalc::calcExposure(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks) {


    const double W = 0.3;
    const double H = 1.8;

    Vec3 samplePoints[9] = {
        {entityPos.x - W, entityPos.y,       entityPos.z - W},
        {entityPos.x + W, entityPos.y,       entityPos.z - W},
        {entityPos.x - W, entityPos.y,       entityPos.z + W},
        {entityPos.x + W, entityPos.y,       entityPos.z + W},
        {entityPos.x - W, entityPos.y + H/2, entityPos.z - W},
        {entityPos.x + W, entityPos.y + H/2, entityPos.z - W},
        {entityPos.x - W, entityPos.y + H/2, entityPos.z + W},
        {entityPos.x + W, entityPos.y + H/2, entityPos.z + W},
        {entityPos.x,     entityPos.y + H/2, entityPos.z    }
    };

    int unblocked = 0;
    for (const Vec3& samplePoint : samplePoints) {
        Vec3 dir = samplePoint - explosionPos;
        double len = dir.length();
        if (len < 0.001) {
            unblocked++;
            continue;
        }

        Vec3 normDir = dir * (1.0 / len);
        bool blocked = false;
        const int STEPS = 12;
        for (int step = 1; step <= STEPS; step++) {
            double t = (double)step / STEPS * len;
            Vec3 p = explosionPos + normDir * t;

            int bx = (int)std::floor(p.x);
            int by = (int)std::floor(p.y);
            int bz = (int)std::floor(p.z);

            for (const Vec3& block : blocks) {
                if ((int)block.x == bx && (int)block.y == by && (int)block.z == bz) {
                    blocked = true;
                    break;
                }
            }
            if (blocked) break;
        }
        if (!blocked) unblocked++;
    }

    return (double)unblocked / 9.0;
}

double DamageCalc::calcRawExplosionDamage(const Vec3& explosionPos, const Vec3& entityPos, const std::vector<Vec3>& blocks) {
    Vec3 center = {entityPos.x, entityPos.y + 0.9, entityPos.z};
    double distance = explosionPos.distanceTo(center);
    double maxDistance = CRYSTAL_EXPLOSION_POWER * 2.0;

    if (distance > maxDistance) return 0.0;

    double exposure = calcExposure(explosionPos, entityPos, blocks);
    double impact = (1.0 - distance / maxDistance) * exposure;



    double baseDamage = (impact * impact + impact) / 2.0 * 7.0 * maxDistance + 1.0;
    return baseDamage;
}

}
