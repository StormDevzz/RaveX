package ravex.modules.render;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.utility.nativelib.NativeLoader;
import ravex.utility.nativelib.NativeLibrary;
public class NoRender extends Module {
    public static final NoRender INSTANCE = new NoRender();
    public final BooleanParameter blockParticles = new BooleanParameter("BlockParticles", true);
    public final BooleanParameter explosions = new BooleanParameter("Explosions", true);
    public final BooleanParameter weather = new BooleanParameter("Weather", true);
    public final BooleanParameter portal = new BooleanParameter("Portal", true);
    public final BooleanParameter sprint = new BooleanParameter("Sprint", false);
    public final BooleanParameter armor = new BooleanParameter("Armor", false);
    public final BooleanParameter items = new BooleanParameter("Items", false);
    public final BooleanParameter tripwire = new BooleanParameter("Tripwire", false);
    public final BooleanParameter signs = new BooleanParameter("Signs", false);
    public final BooleanParameter fog = new BooleanParameter("Fog", true);
    public final BooleanParameter fire = new BooleanParameter("Fire", true);
    public final BooleanParameter inventoryBackground = new BooleanParameter("InventoryBackground", false);
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_norender");
    static {
        try {
            NativeLoader.load();
        } catch (Throwable ignored) {}
        NATIVE.load();
    }

    public static native boolean nativeShouldCull(double x, double y, double z, double camX, double camY, double camZ, double maxDist);
    public static native int nativeOptimizeBudget(int activeCount, int currentFps, int minFps);
    public static native float[] nativeOptimizeFog(float envStart, float envEnd, float rdStart, float rdEnd, float skyEnd, float cloudEnd);
    public static boolean shouldCull(double x, double y, double z, double camX, double camY, double camZ, double maxDist) {
        if (NATIVE.isLoaded()) {
            try {
                return nativeShouldCull(x, y, z, camX, camY, camZ, maxDist);
            } catch (UnsatisfiedLinkError e) {
            }
        }
        double dx = x - camX;
        double dy = y - camY;
        double dz = z - camZ;
        return (dx * dx + dy * dy + dz * dz) > (maxDist * maxDist);
    }
    public static int optimizeBudget(int activeCount, int currentFps, int minFps) {
        if (NATIVE.isLoaded()) {
            try {
                return nativeOptimizeBudget(activeCount, currentFps, minFps);
            } catch (UnsatisfiedLinkError e) {
            }
        }
        if (minFps <= 0) return activeCount;
        if (currentFps < minFps) {
            double ratio = (double) currentFps / (double) minFps;
            if (ratio < 0.1) ratio = 0.1;
            return (int) (activeCount * ratio);
        }
        return activeCount;
    }
    public static float[] optimizeFog(float envStart, float envEnd, float rdStart, float rdEnd, float skyEnd, float cloudEnd) {
        if (NATIVE.isLoaded()) {
            try {
                return nativeOptimizeFog(envStart, envEnd, rdStart, rdEnd, skyEnd, cloudEnd);
            } catch (UnsatisfiedLinkError e) {
            }
        }
        return new float[] { 999999.0f, 999999.0f, 999999.0f, 999999.0f, 999999.0f, 999999.0f };
    }
}
