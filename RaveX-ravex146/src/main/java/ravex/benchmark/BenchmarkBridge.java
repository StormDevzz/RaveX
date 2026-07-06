package ravex.benchmark;

import ravex.utility.misc.NativeLoader;

public class BenchmarkBridge {
    private static boolean nativeAvailable = false;

    static {
        nativeAvailable = NativeLoader.loadLibrary("ravex_benchmark");
    }

    public static boolean isNativeAvailable() {
        return nativeAvailable;
    }

    public static native String runCPUBenchmark();
    public static native String runMemoryBenchmark();
    public static native String runDiskBenchmark();
    public static native String getSystemInfo();
}
