#include "fakepearl.hpp"
#include <cmath>

void calculateVelocity(
    double yaw,
    double pitch,
    double speed,
    double* outVelocity
) {
    double yawRad = yaw * (M_PI / 180.0);
    double pitchRad = pitch * (M_PI / 180.0);

    outVelocity[0] = -std::sin(yawRad) * std::cos(pitchRad) * speed;
    outVelocity[1] = -std::sin(pitchRad) * speed;
    outVelocity[2] = std::cos(yawRad) * std::cos(pitchRad) * speed;
}
