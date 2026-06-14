#include "rocketextender.h"
#include <cmath>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

void calculateRocketBoost(
    double yaw,
    double pitch,
    double currentVx,
    double currentVy,
    double currentVz,
    double boostFactor,
    double* outVelocity
) {
    double yawRad = yaw * (M_PI / 180.0);
    double pitchRad = pitch * (M_PI / 180.0);

    double dx = -std::sin(yawRad) * std::cos(pitchRad);
    double dy = -std::sin(pitchRad);
    double dz = std::cos(yawRad) * std::cos(pitchRad);

    outVelocity[0] = currentVx + dx * boostFactor;
    outVelocity[1] = currentVy + dy * boostFactor;
    outVelocity[2] = currentVz + dz * boostFactor;
}
