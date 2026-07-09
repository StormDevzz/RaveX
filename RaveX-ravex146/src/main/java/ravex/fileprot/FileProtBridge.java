package ravex.fileprot;

import ravex.utility.misc.NativeLoader;

public class FileProtBridge {
    private static boolean nativeAvailable = false;

    static {
        nativeAvailable = NativeLoader.loadLibrary("ravex_fileprot");
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static native boolean nativeInit(String dbPath);
    public static native void nativeShutdown();
    public static native boolean nativeBackupFile(String filePath, String backupDir);
    public static native boolean nativeRestoreFile(String backupPath, String targetPath);
}
