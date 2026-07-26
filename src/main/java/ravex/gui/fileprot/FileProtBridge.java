package ravex.gui.fileprot;

import ravex.utility.nativelib.NativeLibraryUtility;

public class FileProtBridge {
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_fileprot");

    static {
        NATIVE.load();
    }

    public static boolean isNativeAvailable() {
        return NATIVE.isLoaded();
    }

    public static native boolean nativeInit(String dbPath);
    public static native void nativeShutdown();
    public static native boolean nativeBackupFile(String filePath, String backupDir);
    public static native boolean nativeRestoreFile(String backupPath, String targetPath);
}
