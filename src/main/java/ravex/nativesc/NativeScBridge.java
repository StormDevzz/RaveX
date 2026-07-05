package ravex.nativesc;

import ravex.utility.nativelib.NativeLibrary;

public class NativeScBridge {
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_nativesc");

    static {
        NATIVE.load();
    }

    public static boolean isNativeAvailable() {
        return NATIVE.isLoaded();
    }

    public static native boolean nativeInit();
    public static native void nativeShutdown();
    public static native String nativeCaptureScreen(String path);
    public static native String[] nativeListMonitors();
}
