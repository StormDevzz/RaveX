package ravex.utility.render;

import org.joml.Matrix4f;

public class HatUtility {
    private static final double RADIUS = 0.55;
    private static final double HAT_HEIGHT = 0.35;
    private static final int SEGMENTS = 16;
    private static final int LAYERS = 5;
    private static final double DOT_SIZE = 0.09;
    private static final float BASE_ALPHA = 0.85f;

    private HatUtility() {}

    public static void renderHat(Matrix4f modelViewMatrix, double camX, double camY, double camZ,
                                 double headX, double headY, double headZ, int color) {
        renderHat(modelViewMatrix, camX, camY, camZ, headX, headY, headZ, color, 1f);
    }

    public static void renderHat(Matrix4f modelViewMatrix, double camX, double camY, double camZ,
                                 double headX, double headY, double headZ, int color, float scale) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f * BASE_ALPHA;
        if (a <= 0.01f) return;

        float radius = (float) (RADIUS * scale);
        float hatHeight = (float) (HAT_HEIGHT * scale);
        float dotSize = (float) (DOT_SIZE * scale);

        float px = (float)(headX - camX);
        float py = (float)(headY - camY);
        float pz = (float)(headZ - camZ);
        Matrix4f mat = new Matrix4f();

        for (int layer = 0; layer <= LAYERS; layer++) {
            float ly = hatHeight * layer / LAYERS;
            double rAtLayer = layer == LAYERS ? 0.0 : radius * (1.0 - (double)layer / LAYERS);

            for (int seg = 0; seg < SEGMENTS; seg++) {
                double angle = 2.0 * Math.PI * (seg + 0.5 * (layer % 2)) / SEGMENTS;
                float bx = (float)(Math.cos(angle) * rAtLayer);
                float bz = (float)(Math.sin(angle) * rAtLayer);

                mat.identity();
                modelViewMatrix.translate(px + bx - 0.5f, py + ly - 0.5f, pz + bz - 0.5f, mat);
                Render3DUtility.batchFilledBox(mat, dotSize, r, g, b, a, false);
            }
        }
    }
}
