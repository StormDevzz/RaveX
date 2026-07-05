package ravex.utility.shaders;
public class EnergyGlowEffect extends ShaderEffect {
    @Override public EffectType type() { return EffectType.ENERGY_GLOW; }
    @Override public void configure(ShaderConfig config) {}
    @Override public EffectOutput process(EffectInput input) {
        EffectOutput out = new EffectOutput();
        out.color = input.vertex.color;
        out.alpha = 0f;
        out.glow = 0f;
        out.offset = new Vec3(0, 0, 0);
        return out;
    }
    @Override public void reset() {}
}
