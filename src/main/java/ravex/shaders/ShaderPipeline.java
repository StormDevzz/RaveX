package ravex.shaders;

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
        EffectOutput result = new EffectOutput();
        for (ShaderEffect e : effects) {
            EffectOutput out = e.process(input);
            result.color.r *= out.color.r;
            result.color.g *= out.color.g;
            result.color.b *= out.color.b;
            result.color.a *= out.color.a;
            result.alpha *= out.alpha;
            result.glow = Math.max(result.glow, out.glow);
            result.offset.x += out.offset.x;
            result.offset.y += out.offset.y;
            result.offset.z += out.offset.z;
        }
        return result;
    }

    public void processVertices(EffectInput[] inputs, EffectOutput[] outputs) {
        for (int i = 0; i < inputs.length; i++)
            outputs[i] = processVertex(inputs[i]);
    }

    public boolean isDirty() { return dirty; }
    public void markClean() { dirty = false; }
}
