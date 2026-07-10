package ravex.utility.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public class BlockRenderer {
    public static void renderWireframe(VertexConsumer consumer, Matrix4f matrix, double size, float r, float g, float b, float a) {
        renderWireframe(consumer, matrix, size, r, g, b, a, 1.0f);
    }

    public static void renderWireframe(VertexConsumer consumer, Matrix4f matrix, double size, float r, float g, float b, float a, float lineWidth) {
        float min = (float) ((1.0 - size) / 2.0);
        float max = (float) ((1.0 + size) / 2.0);

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        int ia = (int) (a * 255);


        renderLine(consumer, matrix, min, min, min, max, min, min, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, min, min, max, min, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, min, max, min, min, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, min, min, max, min, min, min, ir, ig, ib, ia, lineWidth);


        renderLine(consumer, matrix, min, max, min, max, max, min, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, max, min, max, max, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, max, max, min, max, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, min, max, max, min, max, min, ir, ig, ib, ia, lineWidth);


        renderLine(consumer, matrix, min, min, min, min, max, min, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, min, min, max, max, min, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, max, min, max, max, max, max, ir, ig, ib, ia, lineWidth);
        renderLine(consumer, matrix, min, min, max, min, max, max, ir, ig, ib, ia, lineWidth);
    }

    private static void renderLine(VertexConsumer consumer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b, int a, float lineWidth) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(x2 - x1, y2 - y1, z2 - z1).setLineWidth(lineWidth);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(x2 - x1, y2 - y1, z2 - z1).setLineWidth(lineWidth);
    }


    public static void renderFilledBoxQuads(VertexConsumer consumer, Matrix4f matrix, double size, float r, float g, float b, float a) {
        float min = (float) ((1.0 - size) / 2.0);
        float max = (float) ((1.0 + size) / 2.0);

        int ir = (int)(r * 255);
        int ig = (int)(g * 255);
        int ib = (int)(b * 255);
        int ia = (int)(a * 255);


        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, min, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, min, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, min, max, min).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, max, min, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, min).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, max, max).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, max, min, max).setColor(ir, ig, ib, ia);
    }

    public static void renderSolidBox(VertexConsumer consumer, Matrix4f matrix, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int ir, int ig, int ib, int ia) {

        consumer.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, minX, minY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minX, minY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minX, maxY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, minX, maxY, minZ).setColor(ir, ig, ib, ia);


        consumer.addVertex(matrix, maxX, minY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, maxY, minZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, maxY, maxZ).setColor(ir, ig, ib, ia);
        consumer.addVertex(matrix, maxX, minY, maxZ).setColor(ir, ig, ib, ia);
    }

    public static void renderThickWireframe(VertexConsumer consumer, Matrix4f matrix, double size, float r, float g, float b, float a, float lineWidth) {
        float min = (float) ((1.0 - size) / 2.0);
        float max = (float) ((1.0 + size) / 2.0);

        int ir = (int) (r * 255);
        int ig = (int) (g * 255);
        int ib = (int) (b * 255);
        int ia = (int) (a * 255);


        float t = 0.002f * lineWidth;




        renderSolidBox(consumer, matrix, min, min - t, min - t, max, min + t, min + t, ir, ig, ib, ia);
        renderSolidBox(consumer, matrix, min, max - t, min - t, max, max + t, min + t, ir, ig, ib, ia);
        renderSolidBox(consumer, matrix, min, min - t, max - t, max, min + t, max + t, ir, ig, ib, ia);
        renderSolidBox(consumer, matrix, min, max - t, max - t, max, max + t, max + t, ir, ig, ib, ia);


        renderSolidBox(consumer, matrix, min - t, min, min - t, min + t, max, min + t, ir, ig, ib, ia);
        renderSolidBox(consumer, matrix, max - t, min, min - t, max + t, max, min + t, ir, ig, ib, ia);
        renderSolidBox(consumer, matrix, min - t, min, max - t, min + t, max, max + t, ir, ig, ib, ia);
        renderSolidBox(consumer, matrix, max - t, min, max - t, max + t, max, max + t, ir, ig, ib, ia);


        renderSolidBox(consumer, matrix, min - t, min - t, min, min + t, min + t, max, ir, ig, ib, ia);
        renderSolidBox(consumer, matrix, max - t, min - t, min, max + t, min + t, max, ir, ig, ib, ia);
        renderSolidBox(consumer, matrix, min - t, max - t, min, min + t, max + t, max, ir, ig, ib, ia);
    }

    public static void renderLine3D(com.mojang.blaze3d.vertex.VertexConsumer consumer, Matrix4f matrix,
                                     float x1, float y1, float z1, float x2, float y2, float z2,
                                     int r, int g, int b, int a, float lineWidth) {
        float nx = x2 - x1;
        float ny = y2 - y1;
        float nz = z2 - z1;
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(lineWidth);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(nx, ny, nz).setLineWidth(lineWidth);
    }
}
