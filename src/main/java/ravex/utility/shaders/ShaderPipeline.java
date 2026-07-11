package ravex.utility.shaders;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ShaderPipeline {
    private final List<ShaderEffect> effects = new ArrayList<>();
    private ShaderConfig config = new ShaderConfig();
    private ShaderUniforms uniforms = new ShaderUniforms();
    private boolean dirty;

    public void init() { dirty = true; effects.clear(); }
    public void shutdown() { effects.clear(); }
    public void update(float deltaTime) { dirty = true; }

    public void addEffect(ShaderEffect effect) {
        if (effect != null) {
            effect.configure(config);
            effects.add(effect);
        }
    }

    public void removeEffect(EffectType type) {
        Iterator<ShaderEffect> it = effects.iterator();
        while (it.hasNext()) {
            if (it.next().type() == type) it.remove();
        }
    }

    public void clearEffects() { effects.clear(); }

    public ShaderEffect getEffect(EffectType type) {
        for (ShaderEffect e : effects)
            if (e.type() == type) return e;
        return null;
    }

    public void setConfig(ShaderConfig cfg) {
        this.config = cfg;
        for (ShaderEffect e : effects) e.configure(config);
    }
    public ShaderConfig getConfig() { return config; }

    public void setUniforms(ShaderUniforms u) { this.uniforms = u; dirty = true; }
    public ShaderUniforms getUniforms() { return uniforms; }

    public EffectOutput processVertex(EffectInput input) {
        for (ShaderEffect e : effects) {
            if (e.type() == config.effect) {
                return e.process(input);
            }
        }
        return new EffectOutput();
    }

    public void processVertices(EffectInput[] inputs, EffectOutput[] outputs) {
        for (int i = 0; i < inputs.length; i++)
            outputs[i] = processVertex(inputs[i]);
    }

    public boolean isDirty() { return dirty; }
    public void markClean() { dirty = false; }
}
