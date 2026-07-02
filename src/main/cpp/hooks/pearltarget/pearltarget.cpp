#include "pearltarget.hpp"

namespace ravex {

PearlPrediction predictPearl(Vec3 pos, Vec3 vel, int maxTicks) {
    PearlPrediction result{};
    result.maxHeight = pos.y;
    result.willHitGround = false;
    result.impactTicks = maxTicks;
    result.totalDistance = 0.0;
    Vec3 prev = pos;

    Vec3 p = pos;
    Vec3 v = vel;

    for (int tick = 0; tick < maxTicks; tick++) {
        p = p + v;
        v.y -= 0.03;
        v = v * 0.99;

        if (p.y > result.maxHeight)
            result.maxHeight = p.y;

        result.totalDistance += p.distanceTo(prev);
        prev = p;

        if (p.y < -64.0) {
            result.willHitGround = true;
            result.impactTicks = tick;
            break;
        }
    }

    result.landingPos = p;
    return result;
}

InterceptResult calcIntercept(Vec3 from, Vec3 to, double maxSpeed, int maxTicks) {
    InterceptResult result{};
    Vec3 diff = to - from;
    double dist = diff.length();
    result.estimatedTicks = maxTicks;

    if (dist < 0.1) {
        result.velocity = {0, 0, 0};
        result.requiredSpeed = 0;
        result.reachable = true;
        return result;
    }

    result.requiredSpeed = dist / (double)maxTicks * 20.0;
    result.reachable = result.requiredSpeed <= maxSpeed;

    double scale = (result.reachable ? result.requiredSpeed : maxSpeed) / dist;
    result.velocity = Vec3(diff.x * scale, diff.y * scale, diff.z * scale);

    if (result.reachable) {
        int ticks = (int)(dist / maxSpeed * 20.0);
        result.estimatedTicks = std::min(ticks, maxTicks);
    }

    return result;
}

Vec3 lerp(Vec3 a, Vec3 b, double t) {
    return Vec3(
        a.x + (b.x - a.x) * t,
        a.y + (b.y - a.y) * t,
        a.z + (b.z - a.z) * t
    );
}

}
