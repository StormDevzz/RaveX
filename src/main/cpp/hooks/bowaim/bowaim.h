#pragma once

namespace ravex {

struct BowAimResult {
    bool hit;
    double yaw;
    double pitch;
    int ticks;
};

BowAimResult solveBowAim(
    double playerX, double playerY, double playerZ,
    double targetX, double targetY, double targetZ,
    double targetVelX, double targetVelY, double targetVelZ,
    double targetHeight,
    double arrowSpeed
);

} // namespace ravex
