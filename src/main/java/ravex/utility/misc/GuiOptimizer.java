package ravex.utility.misc;

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

    public static void optimizeHudAnimations(java.util.List<ravex.modules.HudModule> modules) {
        if (modules == null || modules.isEmpty()) return;

        if (!NativeLoader.isNativeAvailable()) {
            for (var m : modules) {
                m.updateAnimation();
            }
            return;
        }

        int count = modules.size();
        float[] displayXs = new float[count];
        float[] displayYs = new float[count];
        int[] targetXs = new int[count];
        int[] targetYs = new int[count];
        boolean[] animInitializeds = new boolean[count];

        for (int i = 0; i < count; i++) {
            var m = modules.get(i);
            displayXs[i] = m.getDisplayX();
            displayYs[i] = m.getDisplayY();
            targetXs[i] = m.getTargetX();
            targetYs[i] = m.getTargetY();
            animInitializeds[i] = m.isAnimInitialized();
        }

        try {
            nativeUpdateHudAnimations(displayXs, displayYs, targetXs, targetYs, animInitializeds, count, 0.25f);

            for (int i = 0; i < count; i++) {
                var m = modules.get(i);
                m.setDisplayX(displayXs[i]);
                m.setDisplayY(displayYs[i]);
                m.setAnimInitialized(animInitializeds[i]);
            }
        } catch (Throwable t) {
            // fallback if anything goes sideways, let's play it safe!
            for (var m : modules) {
                m.updateAnimation();
            }
        }
    }
}
