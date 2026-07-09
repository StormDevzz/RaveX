package ravex.shaders;

public final class ShaderUniforms {
    public Matrix4x4 modelMatrix = Matrix4x4.identity();
    public Matrix4x4 viewMatrix = Matrix4x4.identity();
    public Matrix4x4 projectionMatrix = Matrix4x4.identity();
    public Matrix4x4 mvpMatrix = Matrix4x4.identity();
    public Vec3 cameraPos = new Vec3();
    public Vec3 lightDir = new Vec3(0.5f, 1, 0.5f);
    public float time;
    public float deltaTime;
    public int screenWidth = 854;
    public int screenHeight = 480;
}
