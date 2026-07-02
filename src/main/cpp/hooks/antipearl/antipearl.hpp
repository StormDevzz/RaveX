#ifndef RAVEX_ANTIPEARC_H
#define RAVEX_ANTIPEARC_H

#include <cmath>

namespace ravex {

struct Vec3 {
    double x, y, z;
    Vec3() : x(0), y(0), z(0) {}
    Vec3(double x, double y, double z) : x(x), y(y), z(z) {}
    Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator*(double s) const { return {x * s, y * s, z * s}; }
    double length() const { return std::sqrt(x*x + y*y + z*z); }
};

Vec3 predictPearlLanding(Vec3 pos, Vec3 vel);
bool willHitPlayer(Vec3 landing, Vec3 playerPos, double hitRadius);

} 
#endif
