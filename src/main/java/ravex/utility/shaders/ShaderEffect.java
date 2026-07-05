package ravex.utility.shaders;

public abstract class ShaderEffect {
    public abstract EffectType type();
    public abstract void configure(ShaderConfig config);
    public abstract EffectOutput process(EffectInput input);
    public abstract void reset();
}
