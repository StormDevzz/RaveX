package ravex.utility.misc;

public class NativeLoader {
    private static boolean loaded = false;
    private static boolean nativeAvailable = false;

    private static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }

    private static String getLibName() {
        return isWindows() ? "ravex_jni.dll" : "libravex_jni.so";
    }

    private static String getTempPrefix() {
        return isWindows() ? "ravex_jni" : "libravex_jni";
    }

    private static String getTempSuffix() {
        return isWindows() ? ".dll" : ".so";
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try {
            System.loadLibrary("ravex_jni");
            nativeAvailable = true;
        } catch (UnsatisfiedLinkError e) {
            String libName = getLibName();
            try {
                java.io.InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/" + libName);
                if (in != null) {
                    java.io.File tempFile = java.io.File.createTempFile(getTempPrefix(), getTempSuffix());
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
                    System.err.println("[RaveX] JNI native library not found in JAR assets: " + libName);
                }
                } catch (Throwable ex) {
                System.err.println("[RaveX] WARNING: Failed to dynamically load native library: " + ex.getMessage());
            }
        }
    }

    public static synchronized boolean loadLibrary(String name) {
        try {
            System.loadLibrary(name);
            return true;
        } catch (UnsatisfiedLinkError e) {
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWin = os.contains("win");
            String libName = isWin ? name + ".dll" : "lib" + name + ".so";
            String tempPrefix = isWin ? name : "lib" + name;
            String tempSuffix = isWin ? ".dll" : ".so";
            try {
                java.io.InputStream in = NativeLoader.class.getResourceAsStream("/assets/ravex/natives/" + libName);
                if (in != null) {
                    java.io.File tempFile = java.io.File.createTempFile(tempPrefix, tempSuffix);
                    tempFile.deleteOnExit();
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    System.load(tempFile.getAbsolutePath());
                    return true;
                }
            } catch (Throwable ex) {
                System.err.println("[RaveX] Failed to extract and load native library " + name + ": " + ex.getMessage());
            }
        }
        return false;
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }
}
