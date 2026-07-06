#ifndef RAVEX_SHADER_NOISE_H
#define RAVEX_SHADER_NOISE_H

#include "shader_types.hpp"
#include <cmath>

namespace ravex::shaders {

inline float noise1D(float x) {
    int ix = (int)std::floor(x);
    float fx = x - ix;
    fx = fx * fx * (3.0f - 2.0f * fx);
    float a = (ix * 157 + 137) & 0xff;
    float b = ((ix + 1) * 157 + 137) & 0xff;
    a /= 255.0f; b /= 255.0f;
    return a + (b - a) * fx;
}

inline float noise2D(float x, float y) {
    int ix = (int)std::floor(x);
    int iy = (int)std::floor(y);
    float fx = x - ix, fy = y - iy;
    fx = fx * fx * (3.0f - 2.0f * fx);
    fy = fy * fy * (3.0f - 2.0f * fy);
    auto hash = [](int a, int b) {
        int n = a * 157 + b * 113 + 137;
        return ((n << 13) ^ n) & 0xff;
    };
    float v00 = hash(ix, iy) / 255.0f;
    float v10 = hash(ix+1, iy) / 255.0f;
    float v01 = hash(ix, iy+1) / 255.0f;
    float v11 = hash(ix+1, iy+1) / 255.0f;
    return v00 + (v10-v00)*fx + (v01-v00)*fy + (v00-v10-v01+v11)*fx*fy;
}

inline float noise3D(float x, float y, float z) {
    return noise2D(x + y * 3.7f, y + z * 5.1f + x * 1.3f);
}

inline float perlinNoise(float x, float y, float z) {
    return noise3D(x, y, z);
}

inline float simplexNoise(float x, float y, float z) {
    return noise3D(x * 0.3f, y * 0.3f, z * 0.3f);
}

inline float valueNoise(float x, float y) {
    return noise2D(x, y);
}

inline float fbmNoise(float x, float y, float z, int octaves, float lacunarity, float gain) {
    float value = 0, amplitude = 0.5f, frequency = 1.0f;
    for (int i = 0; i < octaves; i++) {
        value += amplitude * noise3D(x * frequency, y * frequency, z * frequency);
        frequency *= lacunarity;
        amplitude *= gain;
    }
    return value;
}

inline float cellularNoise(float x, float y, float z, float* f2) {
    (void)x; (void)y; (void)z;
    if (f2) *f2 = 0;
    return 0;
}

inline float fractalBrownian(float x, float y, float z, float time, float speed) {
    return fbmNoise(x + time * speed, y + time * speed * 0.7f, z, 4, 2.0f, 0.5f);
}

inline float warpNoise(float x, float y, float z, float amount) {
    float w = noise3D(x, y, z) * amount;
    return noise3D(x + w, y + w * 0.5f, z + w * 0.3f);
}

inline float domainWarp(float x, float y, float z, float warpScale) {
    float dx = noise3D(x + 0.3f, y + 1.1f, z + 2.7f) * warpScale;
    float dy = noise3D(x + 5.3f, y + 9.7f, z + 3.1f) * warpScale;
    float dz = noise3D(x + 7.9f, y + 4.5f, z + 6.2f) * warpScale;
    return noise3D(x + dx, y + dy, z + dz);
}

struct NoiseConfig {
    int octaves = 3;
    float lacunarity = 2.0f;
    float gain = 0.5f;
    float scale = 1.0f;
    float warp = 0.0f;
    float timeSpeed = 0.0f;
};

} // namespace ravex::shaders

#endif
