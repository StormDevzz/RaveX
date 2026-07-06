package ravex.shaders.hand;

import ravex.shaders.*;
import ravex.shaders.nativec.ShaderNative;

public final class FireAuraEffect extends ShaderEffect {
    private ShaderConfig config = new ShaderConfig();

    @Override
    public EffectType type() { return EffectType.FIRE_AURA; }

    @Override
    public void configure(ShaderConfig cfg) { this.config = cfg; }

    @Override
    public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        float t = System.nanoTime() / 1e9f;
        float nx = input.localPos.x * 2f + t * 0.5f;
        float ny = input.localPos.y * 2f + t * 0.3f;
        float nz = input.localPos.z * 2f;
        float heat = ShaderNative.fbmNoise(nx, ny, nz, 3, 2f, 0.5f) * 1.5f;
        heat = Math.max(0, Math.min(1, heat));

        out.color.r = lerp(0.3f, 1f, heat);
        out.color.g = lerp(0.05f, 0.6f, heat);
        out.color.b = lerp(0f, 0.1f, heat);
        out.color.a = 1f;
        out.alpha = lerp(0.3f, 0.9f, heat) * input.intensity;
        out.glow = heat * 0.05f * input.intensity;

        float ox = (ShaderNative.valueNoise2D(input.localPos.y + t, input.localPos.z) - 0.5f) * heat * 0.02f;
        float oy = (ShaderNative.valueNoise2D(input.localPos.z + t, input.localPos.x) - 0.5f) * heat * 0.02f;
        float oz = (ShaderNative.valueNoise2D(input.localPos.x + t, input.localPos.y) - 0.5f) * heat * 0.02f;
        out.offset = new Vec3(ox, oy, oz);

        return out;
    }

    @Override
    public void reset() {}

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
