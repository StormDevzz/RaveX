#pragma once

#include "model_types.h"
#include <cmath>
#include <array>

namespace model {
namespace math {

struct Mat4 {
    std::array<float, 16> m = {
        1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1
    };

    static Mat4 identity() { return Mat4(); }

    static Mat4 translate(float x, float y, float z) {
        Mat4 r;
        r.m[12] = x; r.m[13] = y; r.m[14] = z;
        return r;
    }

    static Mat4 scale(float sx, float sy, float sz) {
        Mat4 r;
        r.m[0] = sx; r.m[5] = sy; r.m[10] = sz;
        return r;
    }

    static Mat4 rotateX(float a) {
        Mat4 r;
        float c = std::cos(a), s = std::sin(a);
        r.m[5] = c; r.m[6] = s; r.m[9] = -s; r.m[10] = c;
        return r;
    }

    static Mat4 rotateY(float a) {
        Mat4 r;
        float c = std::cos(a), s = std::sin(a);
        r.m[0] = c; r.m[2] = -s; r.m[8] = s; r.m[10] = c;
        return r;
    }

    Vec3 transform(const Vec3& v) const {
        return Vec3(
            v.x * m[0] + v.y * m[4] + v.z * m[8]  + m[12],
            v.x * m[1] + v.y * m[5] + v.z * m[9]  + m[13],
            v.x * m[2] + v.y * m[6] + v.z * m[10] + m[14]
        );
    }
};

inline Vec3 add(const Vec3& a, const Vec3& b) { return {a.x+b.x, a.y+b.y, a.z+b.z}; }
inline Vec3 sub(const Vec3& a, const Vec3& b) { return {a.x-b.x, a.y-b.y, a.z-b.z}; }
inline Vec3 mul(const Vec3& v, float s) { return {v.x*s, v.y*s, v.z*s}; }
inline float dot(const Vec3& a, const Vec3& b) { return a.x*b.x + a.y*b.y + a.z*b.z; }
inline float len(const Vec3& v) { return std::sqrt(dot(v, v)); }
inline Vec3 norm(const Vec3& v) { float l = len(v); return l > 0 ? mul(v, 1/l) : v; }
inline Vec3 cross(const Vec3& a, const Vec3& b) {
    return {a.y*b.z - a.z*b.y, a.z*b.x - a.x*b.z, a.x*b.y - a.y*b.x};
}

struct Quat {
    float x = 0, y = 0, z = 0, w = 1;

    static Quat fromEuler(float rx, float ry, float rz) {
        float cx = std::cos(rx/2), sx = std::sin(rx/2);
        float cy = std::cos(ry/2), sy = std::sin(ry/2);
        float cz = std::cos(rz/2), sz = std::sin(rz/2);
        return {
            sx*cy*cz + cx*sy*sz,
            cx*sy*cz - sx*cy*sz,
            cx*cy*sz + sx*sy*cz,
            cx*cy*cz - sx*sy*sz
        };
    }

    Vec3 apply(const Vec3& v) const {
        float tx = 2*(y*v.z - z*v.y);
        float ty = 2*(z*v.x - x*v.z);
        float tz = 2*(x*v.y - y*v.x);
        return Vec3(v.x + w*tx + (y*tz - z*ty),
                    v.y + w*ty + (z*tx - x*tz),
                    v.z + w*tz + (x*ty - y*tx));
    }
};

} 
} 
