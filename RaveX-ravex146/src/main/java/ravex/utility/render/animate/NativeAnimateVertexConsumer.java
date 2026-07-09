package ravex.utility.render.animate;

import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Matrix4fc;
import org.joml.Matrix3x2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;


public class NativeAnimateVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int fillColor;

    public NativeAnimateVertexConsumer(VertexConsumer delegate, int fillColor) {
        this.delegate = delegate;
        this.fillColor = fillColor;
    }

    public NativeAnimateVertexConsumer(VertexConsumer delegate, int fillColor, boolean isHand) {
        this.delegate = delegate;
        this.fillColor = fillColor;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        int na = (fillColor >> 24) & 0xFF;
        int nr = (fillColor >> 16) & 0xFF;
        int ng = (fillColor >> 8) & 0xFF;
        int nb = fillColor & 0xFF;

        delegate.setColor(nr, ng, nb, na);
        return this;
    }

    @Override
    public VertexConsumer setColor(int color) {
        delegate.setColor(fillColor);
        return this;
    }

    @Override
    public VertexConsumer setColor(float r, float g, float b, float a) {
        float na = ((fillColor >> 24) & 0xFF) / 255.0f;
        float nr = ((fillColor >> 16) & 0xFF) / 255.0f;
        float ng = ((fillColor >> 8) & 0xFF) / 255.0f;
        float nb = (fillColor & 0xFF) / 255.0f;

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
        delegate.addVertex(matrix, x, y, z);
        return this;
    }

    @Override
    public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float normalX, float normalY, float normalZ) {
        delegate.addVertex(x, y, z, fillColor, u, v, overlay, light, normalX, normalY, normalZ);
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
