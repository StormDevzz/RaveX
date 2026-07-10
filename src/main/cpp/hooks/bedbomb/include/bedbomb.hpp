#ifndef RAVEX_BEDBOMB_H
#define RAVEX_BEDBOMB_H

#include <cmath>

namespace ravex {

struct Vec3 {
    double x, y, z;
    Vec3() : x(0), y(0), z(0) {}
    Vec3(double x, double y, double z) : x(x), y(y), z(z) {}
    Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator-(const Vec3& o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vec3 operator*(double s) const { return {x * s, y * s, z * s}; }
    double length() const { return std::sqrt(x*x + y*y + z*z); }
    double distTo(const Vec3& o) const {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return std::sqrt(dx*dx + dy*dy + dz*dz);
    }
};

double calcBedDamage(double dist, double explosionPower);
Vec3 findBestBedPlace(Vec3 playerPos, Vec3 enemyPos, double range);

}
#endif
