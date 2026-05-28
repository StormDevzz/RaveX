package ravex.utility.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public class BlockRenderer {
    public static void renderWireframe(VertexConsumer consumer, Matrix4f matrix, double size, float r, float g, float b, float a, float lineWidth) {
        float min = (float) ((1.0 - size) / 2.0);
        float max = (float) ((1.0 + size) / 2.0);

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        int ia = (int) (a * 255);

        // Bottom face edges
        renderLine(consumer, matrix, min, min, min, max, min, min, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, min, min, max, min, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, min, max, min, min, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, min, min, max, min, min, min, ir, ig, ib, ia, lineWidth);

        // Top face edges
        renderLine(consumer, matrix, min, max, min, max, max, min, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, max, min, max, max, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, max, max, min, max, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, min, max, max, min, max, min, ir, ig, ib, ia, lineWidth);

        // Vertical pillar edges
        renderLine(consumer, matrix, min, min, min, min, max, min, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, min, min, max, max, min, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, min, max, max, max, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, min, min, max, min, max, max, ir, ig, ib, ia, lineWidth);
    }

    public static void renderFilled(VertexConsumer consumer, Matrix4f matrix, double size, float r, float g, float b, float a) {
        float min = (float) ((1.0 - size) / 2.0);
        float max = (float) ((1.0 + size) / 2.0);

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        int ia = (int) (a * 255);

        // Down face
        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);

        // Up face
        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);

        // North face
        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);

        // South face
        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);

        // West face
        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);

        // East face
        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
    }

    public static void renderFilledBounds(VertexConsumer consumer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a) {
        float minxf = (float) minX;
        float minyf = (float) minY;
        float minzf = (float) minZ;
        float maxxf = (float) maxX;
        float maxyf = (float) maxY;
        float maxzf = (float) maxZ;

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        int ia = (int) (a * 255);

        // Down face
        consumer.addVertex(matrix, minxf, minyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, minyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, minyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minxf, minyf, maxzf).setColor(ir, ig, ib, ia);

        // Up face
        consumer.addVertex(matrix, minxf, maxyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minxf, maxyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, maxyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, maxyf, minzf).setColor(ir, ig, ib, ia);

        // North face
        consumer.addVertex(matrix, minxf, minyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minxf, maxyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, maxyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, minyf, minzf).setColor(ir, ig, ib, ia);

        // South face
        consumer.addVertex(matrix, minxf, minyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, minyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, maxyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minxf, maxyf, maxzf).setColor(ir, ig, ib, ia);

        // West face
        consumer.addVertex(matrix, minxf, minyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minxf, minyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minxf, maxyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minxf, maxyf, minzf).setColor(ir, ig, ib, ia);

        // East face
        consumer.addVertex(matrix, maxxf, minyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, maxyf, minzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, maxyf, maxzf).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxxf, minyf, maxzf).setColor(ir, ig, ib, ia);
    }

    public static void renderWireframeBounds(VertexConsumer consumer, Matrix4f matrix, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, float r, float g, float b, float a, float lineWidth) {
        float minxf = (float) minX;
        float minyf = (float) minY;
        float minzf = (float) minZ;
        float maxxf = (float) maxX;
        float maxyf = (float) maxY;
        float maxzf = (float) maxZ;

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        int ia = (int) (a * 255);

        // Bottom face edges
        renderLine(consumer, matrix, minxf, minyf, minzf, maxxf, minyf, minzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, maxxf, minyf, minzf, maxxf, minyf, maxzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, maxxf, minyf, maxzf, minxf, minyf, maxzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, minxf, minyf, maxzf, minxf, minyf, minzf, ir, ig, ib, ia, lineWidth);

        // Top face edges
        renderLine(consumer, matrix, minxf, maxyf, minzf, maxxf, maxyf, minzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, maxxf, maxyf, minzf, maxxf, maxyf, maxzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, maxxf, maxyf, maxzf, minxf, maxyf, maxzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, minxf, maxyf, maxzf, minxf, maxyf, minzf, ir, ig, ib, ia, lineWidth);

        // Vertical pillar edges
        renderLine(consumer, matrix, minxf, minyf, minzf, minxf, maxyf, minzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, maxxf, minyf, minzf, maxxf, maxyf, minzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, maxxf, minyf, maxzf, maxxf, maxyf, maxzf, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, minxf, minyf, maxzf, minxf, maxyf, maxzf, ir, ig, ib, ia, lineWidth);
    }

    private static void renderLine(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a, float lineWidth) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len == 0.0f) len = 1.0f;
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(dx / len, dy / len, dz / len).setLineWidth(lineWidth);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(dx / len, dy / len, dz / len).setLineWidth(lineWidth);
    }
}
