package ravex.utility.shaders;

public class FireAuraEffect extends ShaderEffect {
    @Override
    public EffectType type() {
        return EffectType.FIRE_AURA;
    }

    @Override
    public void configure(ShaderConfig config) {}

    @Override
    public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        Vec3 pos = input.worldPos != null ? input.worldPos : input.vertex.position;
        float speed = 3.5f;
        float noise = ravex.utility.shaders.nativec.ShaderNative.perlinNoise(pos.x * 2.0f, pos.y * 2.0f - input.normalizedTime * speed, pos.z * 2.0f);


        float factor = (noise + 1.0f) * 0.5f;
        float r = 1.0f;
        float g = factor * 0.7f;
        float b = factor * factor * 0.2f;

        out.color = new ColorRGBA(r, g, b, 1.0f);
        out.alpha = 1.0f;
        out.glow = factor * 0.3f;

        out.offset = new Vec3(input.vertex.normal.x * 0.05f * factor, input.vertex.normal.y * 0.05f * factor, input.vertex.normal.z * 0.05f * factor);
        return out;
    }

    @Override
    public void reset() {}
}
