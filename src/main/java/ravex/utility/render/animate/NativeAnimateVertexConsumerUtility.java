package ravex.utility.render.animate;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Matrix4fc;
import org.joml.Matrix3x2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import ravex.manager.HandShaderManager;
import ravex.manager.PlayerShaderManager;
import ravex.modules.Modules;
import ravex.modules.render.Shaders;
import ravex.utility.shaders.EffectInput;
import ravex.utility.shaders.EffectOutput;
import ravex.utility.shaders.EffectType;
import ravex.utility.shaders.ShaderConfig;
import ravex.utility.shaders.ShaderPipeline;

public class NativeAnimateVertexConsumerUtility implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int fillColor;
    private final ShaderPipeline pipeline;
    private final float time;
    private final boolean effectActive;
    private final EffectInput effectInput = new EffectInput();
    private float offX;
    private float offY;
    private float offZ;
    private float curX;
    private float curY;
    private float curZ;
    private float curU;
    private float curV;
    private float curNormX;
    private float curNormY;
    private float curNormZ;

    public NativeAnimateVertexConsumerUtility(VertexConsumer delegate, int fillColor) {
        this(delegate, fillColor, false);
    }

    public NativeAnimateVertexConsumerUtility(VertexConsumer delegate, int fillColor, boolean isHand) {
        this.delegate = delegate;
        this.fillColor = fillColor;
        if (isHand) {
            if (!HandShaderManager.isInitialized()) HandShaderManager.init();
            pipeline = HandShaderManager.getPipeline();
        } else {
            if (!PlayerShaderManager.isInitialized()) PlayerShaderManager.init();
            pipeline = PlayerShaderManager.getPipeline();
        }
        this.time = Shaders.tickTime();
        Shaders shaders = Modules.get(Shaders.class);
        boolean active = false;
        if (shaders != null) {
            ShaderConfig cfg = shaders.createConfig();
            pipeline.setConfig(cfg);
            active = cfg.effect != EffectType.NONE;
        }
        effectActive = active;
    }

    private int computeEffectColor(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        offX = 0f;
        offY = 0f;
        offZ = 0f;
        if (!effectActive) return fillColor;

        effectInput.vertex.position.x = x;
        effectInput.vertex.position.y = y;
        effectInput.vertex.position.z = z;
        effectInput.vertex.normal.x = nx;
        effectInput.vertex.normal.y = ny;
        effectInput.vertex.normal.z = nz;
        effectInput.vertex.uv.x = u;
        effectInput.vertex.uv.y = v;
        effectInput.worldPos.x = x;
        effectInput.worldPos.y = y;
        effectInput.worldPos.z = z;
        effectInput.localPos.x = x;
        effectInput.localPos.y = y;
        effectInput.localPos.z = z;
        effectInput.normalizedTime = time;
        effectInput.deltaTime = 0f;
        effectInput.intensity = 1f;

        EffectOutput out = pipeline.processVertex(effectInput);
        float blend = out.alpha;
        if (blend < 0f) blend = 0f;
        if (blend > 1f) blend = 1f;

        float br = ((fillColor >> 16) & 0xFF) / 255f;
        float bg = ((fillColor >> 8) & 0xFF) / 255f;
        float bb = (fillColor & 0xFF) / 255f;
        float ba = ((fillColor >> 24) & 0xFF) / 255f;

        float r = br * (1f - blend) + out.color.r * blend;
        float g = bg * (1f - blend) + out.color.g * blend;
        float b = bb * (1f - blend) + out.color.b * blend;

        if (out.glow > 0f) {
            offX = out.offset.x * out.glow;
            offY = out.offset.y * out.glow;
            offZ = out.offset.z * out.glow;
        }

        int ir = (int) (r * 255f);
        int ig = (int) (g * 255f);
        int ib = (int) (b * 255f);
        int ia = (int) (ba * 255f);
        if (ir > 255) ir = 255;
        if (ig > 255) ig = 255;
        if (ib > 255) ib = 255;
        if (ia > 255) ia = 255;
        return (ia << 24) | (ir << 16) | (ig << 8) | ib;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        curX = x;
        curY = y;
        curZ = z;
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        delegate.setColor(computeEffectColor(curX, curY, curZ, curNormX, curNormY, curNormZ, curU, curV));
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        delegate.setColor(computeEffectColor(curX, curY, curZ, curNormX, curNormY, curNormZ, curU, curV));
        return this;
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        delegate.setColor(computeEffectColor(curX, curY, curZ, curNormX, curNormY, curNormZ, curU, curV));
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        curU = u;
        curV = v;
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(int u, int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int u, int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float x, float y, float z) {
        curNormX = x;
        curNormY = y;
        curNormZ = z;
        delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setOverlay(int overlay) {
        delegate.setOverlay(overlay);
        return this;
    }

    @Override
    public VertexConsumer setLight(int light) {
        delegate.setLight(light);
        return this;
    }

    @Override
    public VertexConsumer addVertex(Matrix4fc matrix, float x, float y, float z) {
        curX = x;
        curY = y;
        curZ = z;
        delegate.addVertex(matrix, x, y, z);
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        int finalColor = computeEffectColor(x, y, z, normalX, normalY, normalZ, u, v);
        delegate.addVertex(x + offX, y + offY, z + offZ, finalColor, u, v, overlay, light, normalX, normalY, normalZ);
    }

    @Override
    public VertexConsumer addVertex(Pose pose, float x, float y, float z) {
        delegate.addVertex(pose, x, y, z);
        return this;
    }

    @Override
    public VertexConsumer addVertex(Pose pose, Vector3f vec) {
        delegate.addVertex(pose, vec);
        return this;
    }

    @Override
    public VertexConsumer addVertex(Vector3fc vec) {
        delegate.addVertex(vec);
        return this;
    }

    @Override
    public VertexConsumer setNormal(Pose pose, float x, float y, float z) {
        delegate.setNormal(pose, x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setNormal(Pose pose, Vector3f vec) {
        delegate.setNormal(pose, vec);
        return this;
    }

    @Override
    public void putBulkData(Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int combinedLight, int combinedOverlay) {
        delegate.putBulkData(pose, quad, red, green, blue, alpha, combinedLight, combinedOverlay);
    }

    @Override
    public void putBulkData(Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int combinedOverlay) {
        delegate.putBulkData(pose, quad, brightness, red, green, blue, alpha, lightmap, combinedOverlay);
    }

    @Override
    public VertexConsumer addVertexWith2DPose(Matrix3x2fc matrix, float x, float y) {
        delegate.addVertexWith2DPose(matrix, x, y);
        return this;
    }
}
