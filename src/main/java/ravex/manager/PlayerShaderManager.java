package ravex.manager;

import ravex.utility.shaders.*;

public final class PlayerShaderManager {
    private static final ShaderPipeline pipeline = new ShaderPipeline();
    private static boolean initialized;

    public static ShaderPipeline getPipeline() { return pipeline; }

    public static void init() {
        if (initialized) return;
        pipeline.init();
        pipeline.addEffect(new FireAuraEffect());
        pipeline.addEffect(new EnergyGlowEffect());
        pipeline.addEffect(new ChromaEffect());
        pipeline.addEffect(new RippleEffect());
        pipeline.addEffect(new PulseEffect());
        initialized = true;
    }

    public static void shutdown() {
        if (!initialized) return;
        pipeline.shutdown();
        initialized = false;
    }

    public static void update(float deltaTime) {
        if (initialized) pipeline.update(deltaTime);
    }

    public static void reset() {
        pipeline.clearEffects();
        pipeline.addEffect(new FireAuraEffect());
        pipeline.addEffect(new EnergyGlowEffect());
        pipeline.addEffect(new ChromaEffect());
        pipeline.addEffect(new RippleEffect());
        pipeline.addEffect(new PulseEffect());
    }

    public static boolean isInitialized() { return initialized; }

    public static class PlayerInput {
        public Vec3[] positions;
        public Vec3[] normals;
        public ColorRGBA[] colors;
        public int vertexCount;
        public Matrix4x4 modelMatrix;
        public Matrix4x4 viewMatrix;
        public Matrix4x4 projectionMatrix;
        public Vec3 cameraPos;
        public Vec3 playerPos;
        public float time;
        public float deltaTime;
        public float intensity;
        public boolean isCrouching;
    }

    public static void processVertices(PlayerInput input) {
        if (!initialized || input.vertexCount <= 0) return;

        ShaderUniforms uniforms = new ShaderUniforms();
        uniforms.time = input.time;
        uniforms.deltaTime = input.deltaTime;
        uniforms.cameraPos = input.cameraPos;
        uniforms.lightDir = new Vec3(0.5f, 1f, 0.5f);
        uniforms.modelMatrix = input.modelMatrix;
        uniforms.viewMatrix = input.viewMatrix;
        uniforms.projectionMatrix = input.projectionMatrix;
        uniforms.mvpMatrix = input.projectionMatrix.mul(input.viewMatrix).mul(input.modelMatrix);

        float heightFactor = input.isCrouching ? 0.6f : 1f;

        for (int i = 0; i < input.vertexCount; i++) {
            Vec4 wp = uniforms.mvpMatrix.transform(new Vec4(
                input.positions[i].x, input.positions[i].y, input.positions[i].z, 1f
            ));
            float invW = 1f / wp.w;
            Vec3 worldPos = new Vec3(wp.x * invW, wp.y * invW, wp.z * invW);

            Vec3 localPos = new Vec3(
                input.positions[i].x - input.playerPos.x,
                (input.positions[i].y - input.playerPos.y) / heightFactor,
                input.positions[i].z - input.playerPos.z
            );

            EffectInput ein = new EffectInput();
            ein.vertex.position = input.positions[i];
            ein.vertex.normal = input.normals[i];
            ein.worldPos = worldPos;
            ein.localPos = localPos;
            ein.normalizedTime = input.time;
            ein.deltaTime = input.deltaTime;
            ein.intensity = input.intensity;
            ein.uniforms = uniforms;

            EffectOutput eout = pipeline.processVertex(ein);

            ColorRGBA c = input.colors[i];
            c.r = clamp(c.r * eout.color.r, 0, 1);
            c.g = clamp(c.g * eout.color.g, 0, 1);
            c.b = clamp(c.b * eout.color.b, 0, 1);
            c.a = clamp(c.a * eout.alpha, 0, 1);
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
