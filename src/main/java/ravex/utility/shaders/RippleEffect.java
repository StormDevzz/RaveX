package ravex.utility.shaders;

public class RippleEffect extends ShaderEffect {
    @Override
    public EffectType type() {
        return EffectType.RIPPLE;
    }

    @Override
    public void configure(ShaderConfig config) {}

    @Override
    public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        Vec3 pos = input.worldPos != null ? input.worldPos : input.vertex.position;
        float speed = 5.0f;
        float dist = (float) Math.sqrt(pos.x * pos.x + pos.z * pos.z);
        float wave = (float) Math.sin(dist * 10.0f - input.normalizedTime * speed);
        float val = wave * 0.5f + 0.5f;


        out.color = new ColorRGBA(0.8f * val, 0.0f, 1.0f, 1.0f);
        out.alpha = 1.0f;
        out.glow = val * 0.2f;
        out.offset = new Vec3(input.vertex.normal.x * 0.03f * wave, input.vertex.normal.y * 0.03f * wave, input.vertex.normal.z * 0.03f * wave);
        return out;
    }

    @Override
    public void reset() {}
}
