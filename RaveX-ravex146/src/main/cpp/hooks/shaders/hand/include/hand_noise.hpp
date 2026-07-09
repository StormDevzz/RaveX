#ifndef RAVEX_HAND_NOISE_H
#define RAVEX_HAND_NOISE_H

#include "shader_types.hpp"
#include "shader_noise.hpp"

namespace ravex::shaders::hand {

void initNoiseSystem();
void updateNoiseSystem(float deltaTime);

float sampleNoise(const Vec3& pos, const NoiseConfig& cfg);
float sampleNoiseWarped(const Vec3& pos, const NoiseConfig& cfg);
void generateDisplacement(const Vec3& pos, Vec3& displacement, float strength, const NoiseConfig& cfg);
float generateTexture(const Vec3& pos, float tiling, float time);
float generateSurfaceNoise(const Vec3& pos, float tiling, const float* octaveWeights, int numOctaves);
float turbulence(const Vec3& pos, float tiling, float time);

void resetNoiseSystem();

} // namespace ravex::shaders::hand

#endif
