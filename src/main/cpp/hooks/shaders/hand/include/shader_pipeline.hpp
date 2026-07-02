#ifndef RAVEX_SHADER_PIPELINE_H
#define RAVEX_SHADER_PIPELINE_H

#include "shader_types.hpp"
#include "shader_effect.hpp"
#include "shader_config.hpp"
#include "shader_transform.hpp"
#include <vector>
#include <memory>

namespace ravex::shaders {

class ShaderPipeline {
public:
    ShaderPipeline();
    ~ShaderPipeline();

    void init();
    void shutdown();
    void update(float deltaTime);

    void addEffect(std::unique_ptr<ShaderEffect> effect);
    void removeEffect(EffectType type);
    void clearEffects();
    ShaderEffect* getEffect(EffectType type);

    void setConfig(const ShaderConfig& config);
    const ShaderConfig& getConfig() const;

    void setUniforms(const ShaderUniforms& uniforms);
    const ShaderUniforms& getUniforms() const;

    EffectOutput processVertex(const EffectInput& input);
    void processVertices(EffectInput* inputs, int count, EffectOutput* outputs);

    void bindTransform(const HandTransform& transform);
    const HandTransform& getBoundTransform() const;

    bool isDirty() const;
    void markClean();

private:
    std::vector<std::unique_ptr<ShaderEffect>> effects;
    ShaderConfig config;
    ShaderUniforms uniforms;
    HandTransform handTransform;
    bool dirty;
    float elapsed;
};

} // namespace ravex::shaders

#endif
