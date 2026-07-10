#ifndef RAVEX_SHADER_EFFECT_H
#define RAVEX_SHADER_EFFECT_H

#include "shader_types.hpp"

namespace ravex::shaders {

struct EffectInput {
    Vertex vertex;
    Vec3 worldPos;
    Vec3 localPos;
    float normalizedTime;
    float deltaTime;
    float intensity;
    const ShaderUniforms* uniforms;
};

struct EffectOutput {
    ColorRGBA color;
    Vec3 offset;
    float alpha;
    float glow;
};

class ShaderEffect {
public:
    virtual ~ShaderEffect() = default;
    virtual EffectType type() const = 0;
    virtual void configure(const ShaderConfig& config) = 0;
    virtual EffectOutput process(const EffectInput& input) = 0;
    virtual void reset() = 0;
};

}

#endif
