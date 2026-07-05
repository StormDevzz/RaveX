package ravex.manager;

import ravex.utility.shaders.*;

public final class HandShaderManager {
    private static final ShaderPipeline pipeline = new ShaderPipeline();
    private static boolean initialized;

    public static ShaderPipeline getPipeline() { return pipeline; }

    public static void init() {
        if (initialized) return;
        pipeline.init();
        pipeline.addEffect(new FireAuraEffect());
        pipeline.addEffect(new EnergyGlowEffect());
        pipeline.addEffect(new ChromaEffect());
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
    }

    public static boolean isInitialized() { return initialized; }

    public static class RenderInput {
        public Vec3[] positions;
        public Vec3[] normals;
        public Vec2[] uvs;
        public ColorRGBA[] colors;
        public int vertexCount;
        public ShaderConfig config;
        public Matrix4x4 modelMatrix;
        public Matrix4x4 viewMatrix;
        public Matrix4x4 projectionMatrix;
        public Vec3 cameraPos;
        public Vec3 lightDir;
        public Vec3 handPos;
        public float time;
        public float deltaTime;
    }

    public static void renderHand(RenderInput input) {
        if (!initialized || !input.config.enabled) return;

        ShaderUniforms uniforms = new ShaderUniforms();
        uniforms.time = input.time;
        uniforms.deltaTime = input.deltaTime;
        uniforms.cameraPos = input.cameraPos;
        uniforms.lightDir = input.lightDir;
        uniforms.modelMatrix = input.modelMatrix;
        uniforms.viewMatrix = input.viewMatrix;
        uniforms.projectionMatrix = input.projectionMatrix;
        uniforms.mvpMatrix = input.projectionMatrix.mul(input.viewMatrix).mul(input.modelMatrix);
        pipeline.setUniforms(uniforms);
        pipeline.setConfig(input.config);

        for (int i = 0; i < input.vertexCount; i++) {
            EffectInput ein = new EffectInput();
            ein.vertex.position = input.positions[i];
            ein.vertex.normal = input.normals[i];
            ein.vertex.uv = input.uvs[i];
            ein.vertex.color = input.colors[i];
            ein.uniforms = uniforms;
            ein.normalizedTime = input.time;
            ein.deltaTime = input.deltaTime;
            ein.intensity = input.config.intensity;
            ein.worldPos = input.positions[i];
            ein.localPos = input.positions[i];

            EffectOutput eout = pipeline.processVertex(ein);

            float blend = eout.alpha;
            ColorRGBA vc = input.colors[i];
            vc.r = vc.r * (1f - blend) + eout.color.r * blend;
            vc.g = vc.g * (1f - blend) + eout.color.g * blend;
            vc.b = vc.b * (1f - blend) + eout.color.b * blend;
            vc.a = 1f;

            if (eout.glow > 0.01f) {
                input.positions[i] = new Vec3(
                    input.positions[i].x + eout.offset.x * eout.glow,
                    input.positions[i].y + eout.offset.y * eout.glow,
                    input.positions[i].z + eout.offset.z * eout.glow
                );
            }
        }
    }
}
