#include "include/shieldfucker_math.hpp"
#include <cmath>

namespace shieldfucker {

double distanceXZ(const Vec3& a, const Vec3& b) {
    double dx = a.x - b.x;
    double dz = a.z - b.z;
    return std::sqrt(dx * dx + dz * dz);
}

double distance(const Vec3& a, const Vec3& b) {
    double dx = a.x - b.x;
    double dy = a.y - b.y;
    double dz = a.z - b.z;
    return std::sqrt(dx * dx + dy * dy + dz * dz);
}

float calculateYaw(const Vec3& from, const Vec3& to) {
    double dx = to.x - from.x;
    double dz = to.z - from.z;
    double yaw = std::atan2(dz, dx) * 180.0 / M_PI - 90.0;
    return normalizeYaw(static_cast<float>(yaw));
}

float calculatePitch(const Vec3& from, const Vec3& to) {
    double dx = to.x - from.x;
    double dy = to.y - from.y;
    double dz = to.z - from.z;
    double horizontal = std::sqrt(dx * dx + dz * dz);
    if (horizontal < 0.001) return 0.0f;
    double pitch = -std::atan2(dy, horizontal) * 180.0 / M_PI;
    return clampPitch(static_cast<float>(pitch));
}

float normalizeYaw(float yaw) {
    while (yaw > 180.0f) yaw -= 360.0f;
    while (yaw < -180.0f) yaw += 360.0f;
    return yaw;
}

float clampPitch(float pitch) {
    if (pitch > 90.0f) return 90.0f;
    if (pitch < -90.0f) return -90.0f;
    return pitch;
}

} 
