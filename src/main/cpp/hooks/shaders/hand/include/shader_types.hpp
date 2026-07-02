#ifndef RAVEX_SHADER_TYPES_H
#define RAVEX_SHADER_TYPES_H

#include <cmath>

namespace ravex::shaders {

struct Vec2 { float x, y; };
struct Vec3 { float x, y, z; };
struct Vec4 { float x, y, z, w; };

struct ColorRGB { float r, g, b; };
struct ColorRGBA { float r, g, b, a; };

struct Matrix4x4 {
    float m[16];

    Matrix4x4() { for (int i = 0; i < 16; i++) m[i] = 0; }

    static Matrix4x4 identity() {
        Matrix4x4 r;
        r.m[0] = r.m[5] = r.m[10] = r.m[15] = 1;
        return r;
    }

    static Matrix4x4 translate(float x, float y, float z) {
        Matrix4x4 r = identity();
        r.m[12] = x; r.m[13] = y; r.m[14] = z;
        return r;
    }

    static Matrix4x4 rotate(float angle, float ax, float ay, float az) {
        float c = std::cos(angle), s = std::sin(angle), t = 1 - c;
        float l = std::sqrt(ax*ax + ay*ay + az*az);
        if (l == 0) return identity();
        float x = ax/l, y = ay/l, z = az/l;
        Matrix4x4 r;
        r.m[0] = t*x*x + c;     r.m[1] = t*x*y - s*z;   r.m[2] = t*x*z + s*y;
        r.m[4] = t*x*y + s*z;   r.m[5] = t*y*y + c;     r.m[6] = t*y*z - s*x;
        r.m[10] = t*z*z + c;    r.m[15] = 1;
        r.m[8] = t*x*z - s*y;   r.m[9] = t*y*z + s*x;
        return r;
    }

    static Matrix4x4 scale(float x, float y, float z) {
        Matrix4x4 r = identity();
        r.m[0] = x; r.m[5] = y; r.m[10] = z;
        return r;
    }

    Matrix4x4 operator*(const Matrix4x4& r) const {
        Matrix4x4 res;
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                for (int k = 0; k < 4; k++)
                    res.m[i*4+j] += m[i*4+k] * r.m[k*4+j];
        return res;
    }

    Vec4 operator*(const Vec4& v) const {
        return {
            m[0]*v.x + m[1]*v.y + m[2]*v.z + m[3]*v.w,
            m[4]*v.x + m[5]*v.y + m[6]*v.z + m[7]*v.w,
            m[8]*v.x + m[9]*v.y + m[10]*v.z + m[11]*v.w,
            m[12]*v.x + m[13]*v.y + m[14]*v.z + m[15]*v.w
        };
    }
};

struct Vertex {
    Vec3 position;
    Vec3 normal;
    Vec2 uv;
    ColorRGBA color;
};

enum class EffectType {
    None,
    FireAura,
    EnergyGlow,
    Ripple,
    Pulse,
    Chroma,
    Gradient,
    Outline,
    Distortion
};

struct ShaderUniforms {
    Matrix4x4 modelMatrix;
    Matrix4x4 viewMatrix;
    Matrix4x4 projectionMatrix;
    Matrix4x4 mvpMatrix;
    Vec3 cameraPos;
    Vec3 lightDir;
    float time;
    float deltaTime;
    int screenWidth;
    int screenHeight;
};

struct ShaderConfig {
    float intensity = 1.0f;
    float speed = 1.0f;
    float scale = 1.0f;
    float opacity = 1.0f;
    bool enabled = false;
    bool throughWalls = false;
    EffectType effect = EffectType::None;
};

} // namespace ravex::shaders

#endif
