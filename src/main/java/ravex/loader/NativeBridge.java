package ravex.loader;

public class NativeBridge {
    private static boolean loaded = false;
    private static String loadError;

    static {
        try {
            System.loadLibrary("ravex_loader");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            loadError = e.getMessage();
            try {
                java.io.InputStream is = NativeBridge.class.getResourceAsStream(
                    "/assets/ravex/natives/libravex_loader.so");
                if (is != null) {
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("ravex_loader", ".so");
                    java.nio.file.Files.copy(is, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    System.load(tmp.toAbsolutePath().toString());
                    tmp.toFile().deleteOnExit();
                    loaded = true;
                    loadError = null;
                }
            } catch (Exception e2) {
                loadError = e2.getMessage();
            }
        }
    }

    public static boolean isLoaded() { return loaded; }
    public static String getLoadError() { return loadError; }

    public static native String runChecks();
    public static native String optimize();
    public static native int trimMemory();
    public static native int setHighPriority();
    public static native String getSystemInfo();
    public static native int getScore();
}
