package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.nativelib.NativeLoader;
import ravex.utility.nativelib.NativeLibraryUtility;
@Module(name = "NoRender", category = "Render")
public class NoRender {
    @Parameter(name = "BlockParticles")
    public boolean blockParticles = true;
    @Parameter(name = "Explosions")
    public boolean explosions = true;
    @Parameter(name = "Weather")
    public boolean weather = true;
    @Parameter(name = "Portal")
    public boolean portal = true;
    @Parameter(name = "Sprint")
    public boolean sprint = false;
    @Parameter(name = "Armor")
    public boolean armor = false;
    @Parameter(name = "Items")
    public boolean items = false;
    @Parameter(name = "Tripwire")
    public boolean tripwire = false;
    @Parameter(name = "Signs")
    public boolean signs = false;
    @Parameter(name = "Fog")
    public boolean fog = true;
    @Parameter(name = "Fire")
    public boolean fire = true;
    @Parameter(name = "InventoryBackground")
    public boolean inventoryBackground = false;
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_norender");
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