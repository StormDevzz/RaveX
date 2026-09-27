package ravex.utility.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

public class BlockRendererUtility {
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

    public static void renderFlatRingBand(VertexConsumer consumer, Matrix4f matrix,
            double cx, double cy, double cz, float rInner, float rOuter, int segs,
            int[] innerColors, int[] outerColors) {
        float y = (float) cy;
        for (int i = 0; i < segs; i++) {
            double a0 = i / (double) segs * Math.PI * 2.0;
            double a1 = (i + 1) / (double) segs * Math.PI * 2.0;
            float inX0 = (float) (cx + Math.cos(a0) * rInner);
            float inZ0 = (float) (cz + Math.sin(a0) * rInner);
            float outX0 = (float) (cx + Math.cos(a0) * rOuter);
            float outZ0 = (float) (cz + Math.sin(a0) * rOuter);
            float inX1 = (float) (cx + Math.cos(a1) * rInner);
            float inZ1 = (float) (cz + Math.sin(a1) * rInner);
            float outX1 = (float) (cx + Math.cos(a1) * rOuter);
            float outZ1 = (float) (cz + Math.sin(a1) * rOuter);
            int inC0 = innerColors[i];
            int outC0 = outerColors[i];
            int inC1 = innerColors[i + 1];
            int outC1 = outerColors[i + 1];
            quadFlat(consumer, matrix, inX0, y, inZ0, outX0, y, outZ0, outX1, y, outZ1, inX1, y, inZ1, inC0, outC0, outC1, inC1);
            quadFlat(consumer, matrix, inX0, y, inZ0, inX1, y, inZ1, outX1, y, outZ1, outX0, y, outZ0, inC0, inC1, outC1, outC0);
        }
    }

    private static void quadFlat(VertexConsumer consumer, Matrix4f matrix,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4,
            int c1, int c2, int c3, int c4) {
        consumer.addVertex(matrix, x1, y1, z1).setColor((c1 >> 16) & 0xFF, (c1 >> 8) & 0xFF, c1 & 0xFF, (c1 >> 24) & 0xFF);
        consumer.addVertex(matrix, x2, y2, z2).setColor((c2 >> 16) & 0xFF, (c2 >> 8) & 0xFF, c2 & 0xFF, (c2 >> 24) & 0xFF);
        consumer.addVertex(matrix, x3, y3, z3).setColor((c3 >> 16) & 0xFF, (c3 >> 8) & 0xFF, c3 & 0xFF, (c3 >> 24) & 0xFF);
        consumer.addVertex(matrix, x4, y4, z4).setColor((c4 >> 16) & 0xFF, (c4 >> 8) & 0xFF, c4 & 0xFF, (c4 >> 24) & 0xFF);
    }

    public static void renderFlatBandQuad(VertexConsumer consumer, Matrix4f matrix,
            float x1, float y1, float z1, float x2, float y2, float z2,
            float x3, float y3, float z3, float x4, float y4, float z4,
            int r, int g, int b, int a) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
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
