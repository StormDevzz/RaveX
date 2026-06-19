package ravex.manager;

import ravex.utility.misc.NativeLoader;

public class NativeManager {
    public static final NativeManager INSTANCE = new NativeManager();

    private boolean checked = false;
    private boolean nativeAvailable = false;

    public static native void nativeCheckNatives();

    private NativeManager() {
        // constructor
    }

    public synchronized void check() {
        if (checked) return;
        checked = true;

        System.out.println("[RaveX] [NativeManager] firing up native checks, let's roll!");

        try {
            // loading the main JNI lib
            NativeLoader.load();
            if (NativeLoader.isNativeAvailable()) {
                System.out.println("[RaveX] [NativeManager] main ravex_jni library loaded successfully, let's go!");
                
                // invoking C++ manager checks
                nativeCheckNatives();
                
                nativeAvailable = true;
            } else {
                System.err.println("[RaveX] [NativeManager] WARNING: native ravex_jni library is MIA!");
            }
        } catch (Throwable t) {
            System.err.println("[RaveX] [NativeManager] damn, check failed for native library: " + t.getMessage());
            t.printStackTrace();
        }
    }

    public boolean isNativeAvailable() {
        return nativeAvailable;
    }
}
