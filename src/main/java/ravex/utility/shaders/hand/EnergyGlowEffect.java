package ravex.utility.shaders.hand;

import ravex.utility.shaders.*;
import ravex.utility.shaders.nativec.ShaderNative;

public final class EnergyGlowEffect extends ShaderEffect {
    private ShaderConfig config = new ShaderConfig();

    @Override
    public EffectType type() { return EffectType.ENERGY_GLOW; }

    @Override
    public void configure(ShaderConfig cfg) { this.config = cfg; }

    @Override
    public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        float t = System.nanoTime() / 1e9f;
        float d = ShaderNative.vec3Length(input.localPos.x, input.localPos.y, input.localPos.z) * 2f;
        float pulse = (float)(Math.sin(d * 3f - t * 4f) * 0.5f + 0.5f);
        float edge = Math.max(0, Math.min(1, 1f - d * 0.8f));
        float glow = pulse * edge * input.intensity;

        out.color.r = lerp(0f, 0.3f, glow);
        out.color.g = lerp(0.3f, 0.8f, glow);
        out.color.b = lerp(1f, 0.6f, glow);
        out.color.a = 1f;
        out.alpha = lerp(0.2f, 0.8f, glow);
        out.glow = glow * 0.08f;

        return out;
    }

    @Override
    public void reset() {}

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
