#include "ecfarmer.hpp"
#include <cmath>

double calcYaw(double dx, double dz) {
    double yaw = atan2(dz, dx) * (180.0 / 3.141592653589793);
    yaw = 90.0 - yaw;
    if (yaw < 0) yaw += 360.0;
    return yaw;
}

double calcPitch(double dx, double dy, double dz) {
    double dist = sqrt(dx * dx + dz * dz);
    double pitch = atan2(-dy, dist) * (180.0 / 3.141592653589793);
    return pitch;
}

float getHorizontalDistance(float dx, float dz) {
    return sqrt(dx * dx + dz * dz);
}

float getVerticalDistance(float dy) {
    return fabs(dy);
}
