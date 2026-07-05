package ravex.manager;

import ravex.utility.misc.NativeLoader;

public class NativeManager {
    public static final NativeManager INSTANCE = new NativeManager();

    private boolean checked = false;
    private boolean nativeAvailable = false;

    public static native void nativeCheckNatives();

    private NativeManager() {
        
    }

    public synchronized void check() {
        if (checked) return;
        checked = true;

        try {
            NativeLoader.load();
            if (NativeLoader.isNativeAvailable()) {
                nativeCheckNatives();
                nativeAvailable = true;
                ravex.RaveX.LOGGER.info("[NativeManager] Native checks passed");
            } else {
                ravex.RaveX.LOGGER.warn("[NativeManager] Native library not available");
            }
        } catch (Throwable t) {
            ravex.RaveX.LOGGER.error("[NativeManager] Check failed: {}", t.getMessage());
        }
    }

    public boolean isNativeAvailable() {
        return nativeAvailable;
    }
}
