package ravex.modules.render;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;

/**
 * Shaders Module
 * Renders players and items with JNI-optimized volumetric waves and custom pulsing color gradients.
 */
public class Shaders extends Module {
    public static final Shaders INSTANCE = new Shaders();

    public static final ThreadLocal<Boolean> RENDERING_PLAYER = ThreadLocal.withInitial(() -> false);
    public static final ThreadLocal<Boolean> RENDERING_HAND = ThreadLocal.withInitial(() -> false);

    public final BooleanParameter players = new BooleanParameter("Players", true);
    public final BooleanParameter throughWalls = new BooleanParameter("Through Walls", false);
    public final ColorParameter fillColor = new ColorParameter("Color", 0x77FF00A4); // default cool translucent violet/magenta

    private static boolean nativeAvailable;

    static {
        ravex.utility.misc.NativeLoader.load();
        nativeAvailable = ravex.utility.misc.NativeLoader.isNativeAvailable();
    }

    private Shaders() {
        super("Shaders", Category.RENDER);
        addParameter(players);
        addParameter(throughWalls);
        addParameter(fillColor);
    }

    @Override
    protected void onEnable() {
        System.out.println("[RaveX-Shaders] Shaders Module ENABLED. JNI Native Active: " + nativeAvailable);
    }

    @Override
    protected void onDisable() {
        System.out.println("[RaveX-Shaders] Shaders Module DISABLED.");
    }

    public static native float nativeCalculateWave(float time, float x, float z);
    public static native int nativeBlendColors(int color1, int color2, float ratio);

    public static float calculateWave(float time, float x, float z) {
        if (nativeAvailable) {
            try {
                return nativeCalculateWave(time, x, z);
            } catch (UnsatisfiedLinkError e) {
                nativeAvailable = false;
            }
        }
        return (float) (Math.sin(time) * 0.012f);
    }

    public static int blendColors(int color1, int color2, float ratio) {
        if (nativeAvailable) {
            try {
                return nativeBlendColors(color1, color2, ratio);
            } catch (UnsatisfiedLinkError e) {
                nativeAvailable = false;
            }
        }
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int)(a1 + (a2 - a1) * ratio);
        int r = (int)(r1 + (r2 - r1) * ratio);
        int g = (int)(g1 + (g2 - g1) * ratio);
        int b = (int)(b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
