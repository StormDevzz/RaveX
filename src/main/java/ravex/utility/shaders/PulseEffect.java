package ravex.utility.shaders;

public class PulseEffect extends ShaderEffect {
    @Override
    public EffectType type() {
        return EffectType.PULSE;
    }

    @Override
    public void configure(ShaderConfig config) {}

    @Override
    public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        float val = (float) Math.sin(input.normalizedTime * 3.0f) * 0.5f + 0.5f;


        out.color = new ColorRGBA(0.2f, 1.0f, 0.2f, 1.0f);
        out.alpha = val * 0.8f + 0.2f;
        out.glow = val * 0.5f;
        out.offset = new Vec3();
        return out;
    }

    @Override
    public void reset() {}
}
