#pragma once

namespace shieldfucker {

struct Vec3 {
    double x, y, z;
};

double distanceXZ(const Vec3& a, const Vec3& b);

double distance(const Vec3& a, const Vec3& b);

float calculateYaw(const Vec3& from, const Vec3& to);

float calculatePitch(const Vec3& from, const Vec3& to);

float normalizeYaw(float yaw);

float clampPitch(float pitch);

} // namespace shieldfucker
