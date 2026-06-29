#include "witherroseaura.h"
#include <cmath>

WitherRoseResult calculateWitherRose(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    double range,
    bool targetFeetIsReplaceable,
    bool supportBlockIsSolid
) {
    WitherRoseResult result = { false, 0, 0, 0, 0, 0, 0 };
    if (!targetFeetIsReplaceable || !supportBlockIsSolid) {
        return result;
    }

    int tx = static_cast<int>(std::floor(targetX));
    int ty = static_cast<int>(std::floor(targetY));
    int tz = static_cast<int>(std::floor(targetZ));

    
    double dx = playerX - (tx + 0.5);
    double dy = (playerY + 1.62) - (ty + 0.5); 
    double dz = playerZ - (tz + 0.5);
    double distSqr = dx*dx + dy*dy + dz*dz;

    if (distSqr <= range * range) {
        result.found = true;
        result.neighborX = tx;
        result.neighborY = ty - 1;
        result.neighborZ = tz;
        result.face = 1; 
        result.targetX = tx;
        result.targetY = ty;
        result.targetZ = tz;
    }
    return result;
}
