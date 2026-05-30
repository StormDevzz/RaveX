package ravex.utility.render.animate;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Matrix4fc;
import org.joml.Matrix3x2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import ravex.modules.render.Shaders;

/**
 * NativeAnimateVertexConsumer
 * Intercepts vertex output streams for players/hands to apply high-performance wave displacements and color blending.
 */
public class NativeAnimateVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int fillColor;
    private final float time;
    private final float pulse;
    private final boolean isHand;

    public NativeAnimateVertexConsumer(VertexConsumer delegate, int fillColor) {
        this(delegate, fillColor, false);
    }

    public NativeAnimateVertexConsumer(VertexConsumer delegate, int fillColor, boolean isHand) {
        this.delegate = delegate;
        this.fillColor = fillColor;
        this.time = (float) ((System.currentTimeMillis() % 100000) / 1000.0);
        this.pulse = (float) (Math.sin(time * 3.0f) * 0.4f + 0.6f);
        this.isHand = isHand;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        // For hands and first-person held items, disable vertex wave displacement to prevent any shaking or rotation jittering
        float wave = isHand ? 0.0f : Shaders.calculateWave(time * 2.0f, x, z);
        delegate.addVertex(x + wave, y, z + wave);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        int baseColor = (a << 24) | (r << 16) | (g << 8) | b;
        int blended = Shaders.blendColors(baseColor, fillColor, pulse);

        int na = (blended >> 24) & 0xFF;
        int nr = (blended >> 16) & 0xFF;
        int ng = (blended >> 8) & 0xFF;
        int nb = blended & 0xFF;

        delegate.setColor(nr, ng, nb, na);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        int blended = Shaders.blendColors(color, fillColor, pulse);
        delegate.setColor(blended);
        return this;
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        int baseColor = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
        int blended = Shaders.blendColors(baseColor, fillColor, pulse);

        float na = ((blended >> 24) & 0xFF) / 255.0f;
        float nr = ((blended >> 16) & 0xFF) / 255.0f;
        float ng = ((blended >> 8) & 0xFF) / 255.0f;
        float nb = (blended & 0xFF) / 255.0f;

        delegate.setColor(nr, ng, nb, na);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
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
        float wave = isHand ? 0.0f : Shaders.calculateWave(time * 2.0f, x, z);
        delegate.addVertex(matrix, x + wave, y, z + wave);
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        float wave = isHand ? 0.0f : Shaders.calculateWave(time * 2.0f, x, z);
        int blended = Shaders.blendColors(color, fillColor, pulse);
        delegate.addVertex(x + wave, y, z + wave, blended, u, v, overlay, light, normalX, normalY, normalZ);
    }

    @Override
    public VertexConsumer addVertex(Pose pose, float x, float y, float z) {
        float wave = isHand ? 0.0f : Shaders.calculateWave(time * 2.0f, x, z);
        delegate.addVertex(pose, x + wave, y, z + wave);
        return this;
    }

    @Override
    public VertexConsumer addVertex(Pose pose, Vector3f vec) {
        float wave = isHand ? 0.0f : Shaders.calculateWave(time * 2.0f, vec.x(), vec.z());
        delegate.addVertex(pose, new Vector3f(vec.x() + wave, vec.y(), vec.z() + wave));
        return this;
    }

    @Override
    public VertexConsumer addVertex(Vector3fc vec) {
        float wave = isHand ? 0.0f : Shaders.calculateWave(time * 2.0f, vec.x(), vec.z());
        delegate.addVertex(new org.joml.Vector3f(vec.x() + wave, vec.y(), vec.z() + wave));
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
