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
        renderLine(consumer, matrix, max, min, min, max, min, max, ir, ig, ig, ia); // correct colors
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
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(x2 - x1, y2 - y1, z2 - z1);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(x2 - x1, y2 - y1, z2 - z1);
    }
}
