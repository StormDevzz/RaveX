package ravex.utility.render.animate;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Matrix4fc;
import org.joml.Matrix3x2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import ravex.modules.Modules;
import ravex.modules.render.Shaders;
import ravex.utility.shaders.ShaderFx;

public class NativeAnimateVertexConsumerUtility implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int baseColor;
    private final int alpha;
    private final int effect;
    private final float time;
    private final boolean effectActive;
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
        this.baseColor = fillColor;
        Shaders shaders = Modules.get(Shaders.class);
        int resolvedAlpha = (fillColor >> 24) & 0xFF;
        int resolvedEffect = ShaderFx.STATIC;
        if (shaders != null) {
            resolvedEffect = shaders.fx();
            resolvedAlpha = shaders.appliedAlpha();
        }
        this.alpha = resolvedAlpha;
        this.effect = resolvedEffect;
        this.time = Shaders.tickTime();
        this.effectActive = resolvedEffect != ShaderFx.STATIC && ShaderFx.available();
    }

    // the effect itself runs in the native engine, java only forwards the vertex
    private int shade(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        offX = 0f;
        offY = 0f;
        offZ = 0f;
        if (!effectActive) return (alpha << 24) | (baseColor & 0x00FFFFFF);
        int packed = ShaderFx.vertex(effect, time, baseColor, alpha, x, y, z, nx, ny, nz, u, v);
        offX = ShaderFx.offsetX();
        offY = ShaderFx.offsetY();
        offZ = ShaderFx.offsetZ();
        return packed;
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
        delegate.setColor(shade(curX, curY, curZ, curNormX, curNormY, curNormZ, curU, curV));
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        delegate.setColor(shade(curX, curY, curZ, curNormX, curNormY, curNormZ, curU, curV));
        return this;
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        delegate.setColor(shade(curX, curY, curZ, curNormX, curNormY, curNormZ, curU, curV));
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
        int finalColor = shade(x, y, z, normalX, normalY, normalZ, u, v);
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
