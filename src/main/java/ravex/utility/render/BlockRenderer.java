package ravex.utility.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public class BlockRenderer {
    public static void renderWireframe(VertexConsumer consumer, Matrix4f matrix, double size, float r, float g, float b, float a) {
        float min = (float) ((1.0 - size) / 2.0);
        float max = (float) ((1.0 + size) / 2.0);

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        int ia = (int) (a * 255);

        // Bottom face edges
        renderLine(consumer, matrix, min, min, min, max, min, min, ir, ig, ib, ia);
        renderLine(consumer, matrix, max, min, min, max, min, max, ir, ig, ib, ia);
        renderLine(consumer, matrix, max, min, max, min, min, max, ir, ig, ib, ia);
        renderLine(consumer, matrix, min, min, max, min, min, min, ir, ig, ib, ia);

        // Top face edges
        renderLine(consumer, matrix, min, max, min, max, max, min, ir, ig, ib, ia);
        renderLine(consumer, matrix, max, max, min, max, max, max, ir, ig, ib, ia);
        renderLine(consumer, matrix, max, max, max, min, max, max, ir, ig, ib, ia);
        renderLine(consumer, matrix, min, max, max, min, max, min, ir, ig, ib, ia);

        // Vertical pillar edges
        renderLine(consumer, matrix, min, min, min, min, max, min, ir, ig, ib, ia);
        renderLine(consumer, matrix, max, min, min, max, max, min, ir, ig, ib, ia);
        renderLine(consumer, matrix, max, min, max, max, max, max, ir, ig, ib, ia);
        renderLine(consumer, matrix, min, min, max, min, max, max, ir, ig, ib, ia);
    }

    private static void renderLine(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(x2 - x1, y2 - y1, z2 - z1).setLineWidth(1.0f);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(x2 - x1, y2 - y1, z2 - z1).setLineWidth(1.0f);
    }

    // For VertexFormat.Mode.QUADS (DEBUG_FILLED_BOX pipeline)
    public static void renderFilledBoxQuads(VertexConsumer consumer, Matrix4f matrix, double size, float r, float g, float b, float a) {
        float min = (float) ((1.0 - size) / 2.0);
        float max = (float) ((1.0 + size) / 2.0);

        int ir = (int)(r * 255);
        int ig = (int)(g * 255);
        int ib = (int)(b * 255);
        int ia = (int)(a * 255);

        // Bottom (y=min)
        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);

        // Top (y=max)
        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);

        // North (z=min)
        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);

        // South (z=max)
        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);

        // West (x=min)
        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);

        // East (x=max)
        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
    }
}
