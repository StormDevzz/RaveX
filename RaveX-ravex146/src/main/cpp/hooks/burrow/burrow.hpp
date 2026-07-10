#ifndef RAVEX_BURROW_H
#define RAVEX_BURROW_H

#include <cmath>
#include <vector>

namespace ravex {

struct BurrowResult {
    double targetX, targetZ;
    double liftY;
    double angle;
};

inline BurrowResult calculateBurrow(double px, double py, double pz, double height, bool autoCenter) {
    BurrowResult r;
    r.targetX = autoCenter ? std::floor(px) + 0.5 : px;
    r.targetZ = autoCenter ? std::floor(pz) + 0.5 : pz;
    r.liftY = py + height;
    r.angle = std::atan2(r.targetX - px, pz - r.targetZ);
    return r;
}

}

#endif
