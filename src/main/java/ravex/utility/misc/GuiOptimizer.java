package ravex.utility.misc;

import ravex.utility.nativelib.NativeLoader;

public class GuiOptimizer {
    public static native void nativeOptimizeGui();

    public static native int nativeOptimizeNameTags(
        double[] cameraPos,
        float[] modelView,
        float[] projection,
        double[] playerViewVec,
        double[] positions,
        double[] textWidths,
        int[] booleans,
        int[] armorCounts,
        int count,
        double scaleParam,
        boolean distanceScaling,
        double maxDistance,
        int guiWidth,
        int guiHeight,
        double[] outLayouts,
        int[] outIndices
    );

    public static native void nativeUpdateHudAnimations(
        float[] displayXs,
        float[] displayYs,
        int[] targetXs,
        int[] targetYs,
        boolean[] animInitializeds,
        int count,
        float speed
    );

    public static native void nativeOptimizeTracers(
        double[] cameraPos,
        float[] modelView,
        float[] projection,
        double[] positions,
        int count,
        int guiWidth,
        int guiHeight,
        double[] outPoints
    );

    static {
        NativeLoader.load();
    }

    private static boolean optimized = false;

    public static void optimize() {
        try {
            if (NativeLoader.isNativeAvailable()) {
                nativeOptimizeGui();
            }
        } catch (Throwable t) {
            System.err.println("[RaveX] Failed to run native GUI optimization: " + t.getMessage());
        }
    }

    public static void optimizeHudAnimations(java.util.List<ravex.modules.Module> modules) {
        if (modules == null || modules.isEmpty()) return;
        for (var m : modules) {
            m.updateAnimation();
        }
    }
}
