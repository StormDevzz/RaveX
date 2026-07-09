#include "phase.hpp"
#include <cmath>

#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif

void calculatePhaseOffset(
    double yaw,
    double pitch,
    double distance,
    double* outOffset
) {
    double yawRad = yaw * (M_PI / 180.0);
    double pitchRad = pitch * (M_PI / 180.0);

    
    outOffset[0] = -std::sin(yawRad) * std::cos(pitchRad) * distance;
    outOffset[1] = -std::sin(pitchRad) * distance;
    outOffset[2] = std::cos(yawRad) * std::cos(pitchRad) * distance;
}
