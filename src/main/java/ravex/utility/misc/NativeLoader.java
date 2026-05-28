package ravex.utility.misc;

public class NativeLoader {
    private static boolean loaded = false;
    private static boolean nativeAvailable = false;

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            // First, attempt loading from the standard library search paths
            System.loadLibrary("ravex_jni");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            // If not found in paths, dynamically extract and load from assets folder inside the JAR
            try {
                java.io.InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/libravex_jni.so");
                if (in != null) {
                    java.io.File tempFile = java.io.File.createTempFile("libravex_jni", ".so");
                    tempFile.deleteOnExit();
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    System.load(tempFile.getAbsolutePath());
                    nativeAvailable = true;
                } else {
                    System.err.println("[RaveX] JNI native library resource not found in JAR assets.");
                }
            } catch (Exception ex) {
                System.err.println("[RaveX] WARNING: Failed to dynamically load native library: " + ex.getMessage());
            }
        }
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }
}
