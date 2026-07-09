#ifndef RAVEX_SHADER_MATH_H
#define RAVEX_SHADER_MATH_H

#include "shader_types.hpp"
#include <cmath>
#include <algorithm>

namespace ravex::shaders {

inline float radians(float deg) { return deg * 0.01745329252f; }
inline float degrees(float rad) { return rad * 57.295779513f; }
inline float clamp(float v, float lo, float hi) { return std::max(lo, std::min(hi, v)); }
inline float lerp(float a, float b, float t) { return a + (b - a) * t; }
inline float saturate(float v) { return clamp(v, 0.0f, 1.0f); }

inline Vec3 vec3Add(const Vec3& a, const Vec3& b) { return {a.x+b.x, a.y+b.y, a.z+b.z}; }
inline Vec3 vec3Sub(const Vec3& a, const Vec3& b) { return {a.x-b.x, a.y-b.y, a.z-b.z}; }
inline Vec3 vec3Mul(const Vec3& a, float s) { return {a.x*s, a.y*s, a.z*s}; }
inline Vec3 vec3Cross(const Vec3& a, const Vec3& b) {
    return {a.y*b.z - a.z*b.y, a.z*b.x - a.x*b.z, a.x*b.y - a.y*b.x};
}
inline float vec3Dot(const Vec3& a, const Vec3& b) { return a.x*b.x + a.y*b.y + a.z*b.z; }
inline float vec3Length(const Vec3& v) { return std::sqrt(v.x*v.x + v.y*v.y + v.z*v.z); }
inline Vec3 vec3Normalize(const Vec3& v) {
    float l = vec3Length(v);
    return l > 0 ? Vec3{v.x/l, v.y/l, v.z/l} : Vec3{0,0,0};
}
inline Vec3 vec3Lerp(const Vec3& a, const Vec3& b, float t) {
    return {lerp(a.x,b.x,t), lerp(a.y,b.y,t), lerp(a.z,b.z,t)};
}
inline float vec3Dist(const Vec3& a, const Vec3& b) { return vec3Length(vec3Sub(a,b)); }

inline float smoothstep(float edge0, float edge1, float x) {
    float t = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
    return t * t * (3.0f - 2.0f * t);
}

inline float easeInOut(float t) { return t * t * (3.0f - 2.0f * t); }
inline float pingPong(float t, float length) { return length - std::abs(t - length); }
inline float frac(float v) { return v - std::floor(v); }
inline float sign(float v) { return (v > 0) - (v < 0); }

} // namespace ravex::shaders

#endif
