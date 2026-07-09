package ravex.nativelayer;

import ravex.loader.NativeBridge;
import ravex.manager.NativeManager;
import ravex.nativesc.NativeScBridge;
import ravex.gui.fileprot.FileProtBridge;
import ravex.utility.nativelib.NativeLoader;
import java.util.HashSet;
import java.util.Set;

public class NativeLayerImpl implements NativeLayer {
    private boolean loaded;
    private final Set<String> loadedLibraries = new HashSet<>();

    @Override
    public boolean isAvailable() {
        return loaded && NativeLoader.isNativeAvailable();
    }

    @Override
    public void load() {
        if (loaded) return;
        NativeLoader.load();
        loaded = true;
    }

    @Override
    public String runChecks() {
        try { return NativeBridge.runChecks(); }
        catch (Throwable t) { return "Error: " + t.getMessage(); }
    }

    @Override
    public String optimize() {
        try { return NativeBridge.optimize(); }
        catch (Throwable t) { return "Error: " + t.getMessage(); }
    }

    @Override
    public int trimMemory() {
        try { return NativeBridge.trimMemory(); }
        catch (Throwable t) { return 0; }
    }

    @Override
    public int setHighPriority() {
        try { return NativeBridge.setHighPriority(); }
        catch (Throwable t) { return 0; }
    }

    @Override
    public String getSystemInfo() {
        try { return NativeBridge.getSystemInfo(); }
        catch (Throwable t) { return "N/A"; }
    }

    @Override
    public int getScore() {
        try { return NativeBridge.getScore(); }
        catch (Throwable t) { return 0; }
    }

    @Override
    public boolean initScreenCapture() {
        try {
            NativeScBridge.isNativeAvailable();
            return NativeScBridge.nativeInit();
        } catch (Throwable t) { return false; }
    }

    @Override
    public void shutdownScreenCapture() {
        try { NativeScBridge.nativeShutdown(); }
        catch (Throwable ignored) {}
    }

    @Override
    public String captureScreen(String path) {
        try { return NativeScBridge.nativeCaptureScreen(path); }
        catch (Throwable t) { return null; }
    }

    @Override
    public String[] listMonitors() {
        try { return NativeScBridge.nativeListMonitors(); }
        catch (Throwable t) { return new String[0]; }
    }

    @Override
    public boolean initFileProtection(String dbPath) {
        try { return FileProtBridge.nativeInit(dbPath); }
        catch (Throwable t) { return false; }
    }

    @Override
    public void shutdownFileProtection() {
        try { FileProtBridge.nativeShutdown(); }
        catch (Throwable ignored) {}
    }

    @Override
    public boolean backupFile(String filePath, String backupDir) {
        try { return FileProtBridge.nativeBackupFile(filePath, backupDir); }
        catch (Throwable t) { return false; }
    }

    @Override
    public boolean restoreFile(String backupPath, String targetPath) {
        try { return FileProtBridge.nativeRestoreFile(backupPath, targetPath); }
        catch (Throwable t) { return false; }
    }

    @Override
    public void checkNatives() {
        try {
            NativeLoader.load();
            if (NativeLoader.isNativeAvailable()) {
                NativeManager.nativeCheckNatives();
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean loadLibrary(String name) {
        boolean success = NativeLoader.loadLibrary(name);
        if (success) loadedLibraries.add(name);
        return success;
    }

    @Override
    public boolean isLibraryLoaded(String name) {
        return loadedLibraries.contains(name);
    }
}
