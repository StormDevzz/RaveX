#pragma once
#include <cmath>
#include <vector>

namespace ravex {

struct Vec3 {
    double x, y, z;
    Vec3() : x(0), y(0), z(0) {}
    Vec3(double x, double y, double z) : x(x), y(y), z(z) {}
    Vec3 operator+(const Vec3& o) const { return {x+o.x, y+o.y, z+o.z}; }
    Vec3 operator-(const Vec3& o) const { return {x-o.x, y-o.y, z-o.z}; }
    Vec3 operator*(double s) const { return {x*s, y*s, z*s}; }
    double length() const { return std::sqrt(x*x + y*y + z*z); }
    double lengthSq() const { return x*x + y*y + z*z; }
    double distanceTo(const Vec3& o) const { return (*this - o).length(); }
};

struct PearlPrediction {
    Vec3  landingPos;
    int   impactTicks;
    double maxHeight;
    bool  willHitGround;
    double totalDistance;
};

struct InterceptResult {
    Vec3  velocity;
    double requiredSpeed;
    int   estimatedTicks;
    bool  reachable;
};

PearlPrediction predictPearl(Vec3 pos, Vec3 vel, int maxTicks);

InterceptResult calcIntercept(Vec3 from, Vec3 to, double maxSpeed, int maxTicks);

Vec3 lerp(Vec3 a, Vec3 b, double t);

}
