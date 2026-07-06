package ravex.nativesc;

import ravex.utility.misc.NativeLoader;

public class NativeScBridge {
    private static boolean nativeAvailable = false;

    static {
        nativeAvailable = NativeLoader.loadLibrary("ravex_nativesc");
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static native boolean nativeInit();
    public static native void nativeShutdown();
    public static native String nativeCaptureScreen(String path);
    public static native String[] nativeListMonitors();
}
