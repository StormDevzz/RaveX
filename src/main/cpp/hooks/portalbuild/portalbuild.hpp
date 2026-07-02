#ifndef RAVEX_PORTALBUILD_H
#define RAVEX_PORTALBUILD_H

#include <cmath>
#include <vector>

namespace ravex {

struct Vec3 {
    double x, y, z;
    Vec3() : x(0), y(0), z(0) {}
    Vec3(double x, double y, double z) : x(x), y(y), z(z) {}
    Vec3 operator+(const Vec3& o) const { return {x + o.x, y + o.y, z + o.z}; }
    Vec3 operator-(const Vec3& o) const { return {x - o.x, y - o.y, z - o.z}; }
    Vec3 operator*(double s) const { return {x * s, y * s, z * s}; }
    double distTo(const Vec3& o) const {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return std::sqrt(dx*dx + dy*dy + dz*dz);
    }
    double distToSqr(const Vec3& o) const {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx*dx + dy*dy + dz*dz;
    }
};

struct PortalPos {
    int x, y, z;
    double score;
    PortalPos() : x(0), y(0), z(0), score(-1.0) {}
    PortalPos(int x, int y, int z, double s) : x(x), y(y), z(z), score(s) {}
};

PortalPos findBestPortalPos(
    double playerX, double playerY, double playerZ,
    double playerYaw,
    double minDistFromPlayer,
    double maxDistFromPlayer,
    double avoidPortalRange,
    const double* existingPortalPositions,
    int portalCount,
    double* groundHeights,
    int groundCount
);

} 
#endif
