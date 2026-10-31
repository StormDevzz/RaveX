#ifndef RAVEX_SHADER_FX_HPP
#define RAVEX_SHADER_FX_HPP

#include "shader_color.hpp"
#include "shader_noise.hpp"
#include <algorithm>
#include <cmath>

namespace ravex::shaders::fx {

enum Effect {
    EFFECT_STATIC      = 0,
    EFFECT_FIRE_AURA   = 1,
    EFFECT_ENERGY_GLOW = 2,
    EFFECT_CHROMA      = 3
};

struct Context {
    int   effect = EFFECT_STATIC;
    float time   = 0.0f;
    float baseR  = 1.0f;
    float baseG  = 1.0f;
    float baseB  = 1.0f;
    int   alpha  = 255;
    bool  tint   = false;
};

struct Result {
    int   r = 255, g = 255, b = 255, a = 255;
    float offsetX = 0.0f, offsetY = 0.0f, offsetZ = 0.0f;
    float glow = 0.0f;
};

struct Raw {
    float r = 1.0f, g = 1.0f, b = 1.0f;
    float blend = 1.0f;
    float offsetX = 0.0f, offsetY = 0.0f, offsetZ = 0.0f;
    float glow = 0.0f;
};

inline float clamp01(float v) {
    return v < 0.0f ? 0.0f : (v > 1.0f ? 1.0f : v);
}

inline int toByte(float v) {
    int b = (int)(v * 255.0f);
    return b < 0 ? 0 : (b > 255 ? 255 : b);
}

inline Raw fireAura(float x, float y, float z, float nx, float ny, float nz, float time) {
    float noise = perlinNoise(x * 2.0f, y * 2.0f - time * 3.5f, z * 2.0f);
    float factor = (noise + 1.0f) * 0.5f;
    Raw out;
    out.r = 1.0f;
    out.g = factor * 0.7f;
    out.b = factor * factor * 0.2f;
    out.glow = factor * 0.3f;
    out.offsetX = nx * 0.05f * factor;
    out.offsetY = ny * 0.05f * factor;
    out.offsetZ = nz * 0.05f * factor;
    return out;
}

inline Raw energyGlow(float y, float nx, float ny, float nz, float time) {
    float val = std::sin(y * 3.0f - time * 3.0f) * 0.5f + 0.5f;
    Raw out;
    out.r = 0.0f;
    out.g = 0.7f + val * 0.3f;
    out.b = 1.0f;
    out.glow = val * 0.4f;
    out.offsetX = nx * 0.02f * val;
    out.offsetY = ny * 0.02f * val;
    out.offsetZ = nz * 0.02f * val;
    return out;
}

inline Raw chroma(float x, float y, float z, float time) {
    float hue = (x + y + z) * 0.5f + time * 2.0f;
    hue -= std::floor(hue);
    ColorRGB c = hsbToRgb({hue, 0.85f, 1.0f});
    Raw out;
    out.r = c.r;
    out.g = c.g;
    out.b = c.b;
    return out;
}

inline Result process(const Context& ctx, float x, float y, float z,
                      float nx, float ny, float nz, float u, float v) {
    (void)u; (void)v;
    Result out;
    out.a = ctx.alpha;
    if (ctx.effect == EFFECT_STATIC) {
        out.r = toByte(ctx.baseR);
        out.g = toByte(ctx.baseG);
        out.b = toByte(ctx.baseB);
        return out;
    }

    Raw raw;
    if (ctx.effect == EFFECT_FIRE_AURA)        raw = fireAura(x, y, z, nx, ny, nz, ctx.time);
    else if (ctx.effect == EFFECT_ENERGY_GLOW) raw = energyGlow(y, nx, ny, nz, ctx.time);
    else                                       raw = chroma(x, y, z, ctx.time);

    float blend = clamp01(raw.blend);
    float lum = raw.r * 0.299f + raw.g * 0.587f + raw.b * 0.114f;
    // picked colour stays, only brightness breathes
    float low  = ctx.tint ? 0.55f : 0.45f;
    float high = ctx.tint ? 0.45f : 0.55f;
    float k = low + high * std::min(1.0f, lum * blend + (1.0f - blend));
    out.r = toByte(ctx.baseR * k);
    out.g = toByte(ctx.baseG * k);
    out.b = toByte(ctx.baseB * k);

    if (raw.glow > 0.0f) {
        out.offsetX = raw.offsetX * raw.glow;
        out.offsetY = raw.offsetY * raw.glow;
        out.offsetZ = raw.offsetZ * raw.glow;
    }
    out.glow = raw.glow;
    return out;
}

}

#endif
