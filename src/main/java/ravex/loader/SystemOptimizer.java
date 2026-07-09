package ravex.loader;

public class SystemOptimizer {
    private boolean nativeAvailable;

    public SystemOptimizer(boolean nativeAvailable) {
        this.nativeAvailable = nativeAvailable;
    }

    public int runChecks(LoaderCallback callback, String windowDetails) {
        callback.updateStatus("Checking system...", 5);

        if (nativeAvailable) {
            try {
                String json = NativeBridge.runChecks();
                int score = NativeBridge.getScore();
                callback.setSystemScore(score);
                callback.setExtraInfo("Score: " + score + "/100");

                Runtime rt = Runtime.getRuntime();
                long maxMem = rt.maxMemory() / (1024 * 1024);
                long totalMem = rt.totalMemory() / (1024 * 1024);
                long freeMem = rt.freeMemory() / (1024 * 1024);
                long usedMem = totalMem - freeMem;

                String info = windowDetails + " | Heap: " + usedMem + "/" + maxMem + " MB";
                callback.setSystemInfo(info);
                callback.updateStatus("System checked: " + score + "/100", 20);

                return score;
            } catch (Exception e) {
                callback.updateStatus("Native checks unavailable, using Java fallback", 15);
                return javaFallbackChecks(callback, windowDetails);
            }
        } else {
            callback.setExtraInfo("Native optimizer not loaded");
            callback.updateStatus("Native library unavailable", 15);
            return javaFallbackChecks(callback, windowDetails);
        }
    }

    private int javaFallbackChecks(LoaderCallback callback, String windowDetails) {
        Runtime rt = Runtime.getRuntime();
        long maxMem = rt.maxMemory() / (1024 * 1024);
        long totalMem = rt.totalMemory() / (1024 * 1024);
        long freeMem = rt.freeMemory() / (1024 * 1024);
        long usedMem = totalMem - freeMem;

        String info = windowDetails + " | Heap: " + usedMem + "/" + maxMem + " MB";
        callback.setSystemInfo(info);

        int score = 100;
        if (usedMem > maxMem * 0.8) score -= 20;
        else if (usedMem > maxMem * 0.6) score -= 10;

        callback.setSystemScore(score);
        callback.setExtraInfo("Score: " + score + "/100 (Java fallback)");
        callback.updateStatus("System check done: " + score + "/100", 20);
        return score;
    }

    public void optimize(LoaderCallback callback) {
        callback.updateStatus("Optimizing system...", 25);

        if (nativeAvailable) {
            try {
                NativeBridge.trimMemory();
                callback.updateStatus("Memory trimmed", 30);

                NativeBridge.setHighPriority();
                callback.updateStatus("Priority adjusted", 35);

                NativeBridge.optimize();
                callback.updateStatus("System optimized", 40);
            } catch (Exception e) {
                callback.updateStatus("Optimization skipped", 40);
            }
        } else {
            System.gc();
            callback.updateStatus("GC completed", 35);
            callback.updateStatus("Optimization skipped (no native)", 40);
        }
    }
}
