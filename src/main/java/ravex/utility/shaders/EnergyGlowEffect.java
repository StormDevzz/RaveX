package ravex.utility.shaders;

public class EnergyGlowEffect extends ShaderEffect {
    @Override
    public EffectType type() {
        return EffectType.ENERGY_GLOW;
    }

    @Override
    public void configure(ShaderConfig config) {}

    @Override
    public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        Vec3 pos = input.worldPos != null ? input.worldPos : input.vertex.position;
        float speed = 3.0f;
        float t = input.normalizedTime * speed;
        float val = (float) Math.sin(pos.y * 3.0f - t) * 0.5f + 0.5f;


        out.color = new ColorRGBA(0.0f, 0.7f + val * 0.3f, 1.0f, 1.0f);
        out.alpha = 1.0f;
        out.glow = val * 0.4f;
        out.offset = new Vec3(input.vertex.normal.x * 0.02f * val, input.vertex.normal.y * 0.02f * val, input.vertex.normal.z * 0.02f * val);
        return out;
    }

    @Override
    public void reset() {}
}
