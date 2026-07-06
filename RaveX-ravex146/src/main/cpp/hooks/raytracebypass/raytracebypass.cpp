#include "raytracebypass.hpp"
#include <cmath>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

void calculateBypassRotation(
    double playerX,
    double playerY,
    double playerZ,
    double blockX,
    double blockY,
    double blockZ,
    double* outRotation
) {
    double targetX = blockX + 0.5;
    double targetY = blockY + 0.5;
    double targetZ = blockZ + 0.5;

    double dx = targetX - playerX;
    double dy = targetY - playerY;
    double dz = targetZ - playerZ;

    double r = std::sqrt(dx * dx + dy * dy + dz * dz);
    if (r < 0.001) {
        outRotation[0] = 0.0;
        outRotation[1] = 0.0;
        return;
    }

    double yaw = std::atan2(dz, dx) * (180.0 / M_PI) - 90.0;
    double pitch = -std::asin(dy / r) * (180.0 / M_PI);

    while (yaw < -180.0) yaw += 360.0;
    while (yaw > 180.0) yaw -= 360.0;

    outRotation[0] = yaw;
    outRotation[1] = pitch;
}
