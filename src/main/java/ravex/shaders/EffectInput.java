package ravex.shaders;

public final class EffectInput {
    public Vertex vertex;
    public Vec3 worldPos;
    public Vec3 localPos;
    public float normalizedTime;
    public float deltaTime;
    public float intensity;
    public ShaderUniforms uniforms;

    public EffectInput() {
        vertex = new Vertex();
        worldPos = new Vec3();
        localPos = new Vec3();
        uniforms = new ShaderUniforms();
    }
}
