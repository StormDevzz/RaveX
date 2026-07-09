package ravex.nativelayer;

public interface NativeLayer {
    boolean isAvailable();

    void load();

    String runChecks();
    String optimize();
    int trimMemory();
    int setHighPriority();
    String getSystemInfo();
    int getScore();

    boolean initScreenCapture();
    void shutdownScreenCapture();
    String captureScreen(String path);
    String[] listMonitors();

    boolean initFileProtection(String dbPath);
    void shutdownFileProtection();
    boolean backupFile(String filePath, String backupDir);
    boolean restoreFile(String backupPath, String targetPath);

    void checkNatives();

    boolean loadLibrary(String name);
    boolean isLibraryLoaded(String name);
}
