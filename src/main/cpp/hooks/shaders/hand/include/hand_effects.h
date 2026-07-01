#ifndef RAVEX_HAND_EFFECTS_H
#define RAVEX_HAND_EFFECTS_H

#include "shader_types.h"
#include "shader_effect.h"
#include <memory>

namespace ravex::shaders::hand {

void initEffects();
void updateEffects(float deltaTime);
void setActiveEffect(EffectType type);
EffectType getActiveEffect();

EffectOutput processEffect(const EffectInput& input);

void processFireAura(const EffectInput& input, EffectOutput& out);
void processEnergyGlow(const EffectInput& input, EffectOutput& out);
void processRipple(const EffectInput& input, EffectOutput& out);
void processPulse(const EffectInput& input, EffectOutput& out);
void processChroma(const EffectInput& input, EffectOutput& out);

void resetEffects();

class FireAuraEffect : public ShaderEffect {
public:
    EffectType type() const override { return EffectType::FireAura; }
    void configure(const ShaderConfig& cfg) override { config = cfg; }
    EffectOutput process(const EffectInput& input) override;
    void reset() override {}
private:
    ShaderConfig config;
};

class EnergyGlowEffect : public ShaderEffect {
public:
    EffectType type() const override { return EffectType::EnergyGlow; }
    void configure(const ShaderConfig& cfg) override { config = cfg; }
    EffectOutput process(const EffectInput& input) override;
    void reset() override {}
private:
    ShaderConfig config;
};

class ChromaEffect : public ShaderEffect {
public:
    EffectType type() const override { return EffectType::Chroma; }
    void configure(const ShaderConfig& cfg) override { config = cfg; }
    EffectOutput process(const EffectInput& input) override;
    void reset() override {}
private:
    ShaderConfig config;
};

} // namespace ravex::shaders::hand

#endif
