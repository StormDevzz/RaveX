#ifndef RAVEX_SHADER_CONFIG_H
#define RAVEX_SHADER_CONFIG_H

#include "shader_types.hpp"
#include "shader_color.hpp"

namespace ravex::shaders {

struct PresetConfig {
    const char* name;
    EffectType effect;
    float intensity;
    float speed;
    float scale;
    float opacity;
    ColorRGBA primaryColor;
    ColorRGBA secondaryColor;
};

static const PresetConfig PRESETS[] = {
    { "Fire Aura",   EffectType::FireAura,    1.0f, 1.2f, 1.0f, 0.8f, {1.0f,0.2f,0.0f,1.0f}, {1.0f,0.8f,0.0f,1.0f} },
    { "Energy Glow", EffectType::EnergyGlow,   0.8f, 0.6f, 1.2f, 0.6f, {0.0f,0.5f,1.0f,1.0f}, {0.5f,0.0f,1.0f,1.0f} },
    { "Ripple",      EffectType::Ripple,       0.6f, 0.8f, 1.5f, 0.5f, {0.0f,1.0f,0.8f,1.0f}, {0.0f,0.6f,1.0f,1.0f} },
    { "Pulse",       EffectType::Pulse,        1.2f, 2.0f, 1.0f, 0.9f, {1.0f,0.0f,0.5f,1.0f}, {1.0f,0.5f,1.0f,1.0f} },
    { "Chroma",      EffectType::Chroma,       1.0f, 1.5f, 1.0f, 0.7f, {1.0f,1.0f,1.0f,1.0f}, {0.0f,0.0f,0.0f,1.0f} },
};

constexpr int PRESET_COUNT = sizeof(PRESETS) / sizeof(PRESETS[0]);

struct RuntimeConfig {
    ShaderConfig current;
    int activePreset;
    bool useCustom;
    float globalIntensity;
    float globalSpeed;
    float viewBobbingInfluence;
    bool bindToHandLocal;
    bool smoothFollow;
    float followSpeed;
};

inline ShaderConfig getDefaultConfig() {
    return {};
}

inline ShaderConfig getPresetConfig(int index) {
    if (index < 0 || index >= PRESET_COUNT) return {};
    ShaderConfig cfg;
    cfg.intensity = PRESETS[index].intensity;
    cfg.speed = PRESETS[index].speed;
    cfg.scale = PRESETS[index].scale;
    cfg.opacity = PRESETS[index].opacity;
    cfg.enabled = true;
    cfg.effect = PRESETS[index].effect;
    return cfg;
}

inline RuntimeConfig getDefaultRuntimeConfig() {
    RuntimeConfig rc;
    rc.current = {};
    rc.activePreset = -1;
    rc.useCustom = false;
    rc.globalIntensity = 1.0f;
    rc.globalSpeed = 1.0f;
    rc.viewBobbingInfluence = 0.3f;
    rc.bindToHandLocal = true;
    rc.smoothFollow = true;
    rc.followSpeed = 5.0f;
    return rc;
}

} // namespace ravex::shaders

#endif
