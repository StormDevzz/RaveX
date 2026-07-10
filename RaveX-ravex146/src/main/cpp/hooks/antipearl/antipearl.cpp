#include "antipearl.hpp"
#include <cmath>

namespace ravex {

Vec3 predictPearlLanding(Vec3 pos, Vec3 vel) {
    Vec3 p = pos;
    Vec3 v = vel;

    for (int tick = 0; tick < 300; tick++) {
        p = p + v;
        v.y -= 0.03;
        v = v * 0.99;
        if (p.y < -64.0) break;
    }
    return p;
}

bool willHitPlayer(Vec3 landing, Vec3 playerPos, double hitRadius) {
    double dx = landing.x - playerPos.x;
    double dy = landing.y - playerPos.y;
    double dz = landing.z - playerPos.z;
    return (dx * dx + dy * dy + dz * dz) <= (hitRadius * hitRadius);
}

}
