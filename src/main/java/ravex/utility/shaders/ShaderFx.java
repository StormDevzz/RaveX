package ravex.utility.shaders;

import ravex.modules.render.Shaders;
import ravex.utility.shaders.nativec.ShaderNative;

// thin front end of the native effect engine - no effect math on the java side.
public final class ShaderFx {
    public static final int STATIC = 0;
    public static final int FIRE_AURA = 1;
    public static final int ENERGY_GLOW = 2;
    public static final int CHROMA = 3;

    private static final float[] OUT = new float[8];

    private ShaderFx() {}

    public static int idOf(String mode) {
        if (mode == null) return STATIC;
        return switch (mode) {
            case "FireAura" -> FIRE_AURA;
            case "EnergyGlow" -> ENERGY_GLOW;
            case "Chroma" -> CHROMA;
            default -> STATIC;
        };
    }

    public static boolean available() {
        return Shaders.nativeReady();
    }

    // shades one vertex, returns the packed argb
    public static int vertex(int effect, float time, int color, int alpha,
                             float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        ShaderNative.nFxVertex(effect, time, color, alpha, x, y, z, nx, ny, nz, u, v, OUT);
        return ((int) OUT[3] << 24) | ((int) OUT[0] << 16) | ((int) OUT[1] << 8) | (int) OUT[2];
    }

    public static int tint(int effect, float time, int color, int alpha, float x, float y, float z) {
        return ShaderNative.nFxTint(effect, time, color, alpha, x, y, z);
    }

    public static float offsetX() { return OUT[4]; }
    public static float offsetY() { return OUT[5]; }
    public static float offsetZ() { return OUT[6]; }
}
