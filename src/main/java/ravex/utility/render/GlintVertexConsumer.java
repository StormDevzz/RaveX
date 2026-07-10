package ravex.utility.render;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class GlintVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int color;

    public GlintVertexConsumer(VertexConsumer delegate, int color) {
        this.delegate = delegate;
        this.color = color;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        int tr = (color >> 16) & 0xFF;
        int tg = (color >> 8) & 0xFF;
        int tb = color & 0xFF;
        int ta = (color >> 24) & 0xFF;
        if (ta == 0) ta = a;
        delegate.setColor(tr, tg, tb, ta);
        return this;
    }

    @Override
    public VertexConsumer setColor(int packedColor) {
        int a = (packedColor >> 24) & 0xFF;
        int tr = (color >> 16) & 0xFF;
        int tg = (color >> 8) & 0xFF;
        int tb = color & 0xFF;
        int ta = (color >> 24) & 0xFF;
        if (ta == 0) ta = a;
        delegate.setColor((ta << 24) | (tr << 16) | (tg << 8) | tb);
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
    public VertexConsumer setLineWidth(float width) {
        delegate.setLineWidth(width);
        return this;
    }
}
