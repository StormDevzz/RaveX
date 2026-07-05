package ravex.benchmark;

import ravex.utility.nativelib.NativeLibrary;

public class BenchmarkBridge {
    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_benchmark");

    static {
        NATIVE.load();
    }

    public static boolean isNativeAvailable() {
        return NATIVE.isLoaded();
    }

    public static native String runCPUBenchmark();
    public static native String runMemoryBenchmark();
    public static native String runDiskBenchmark();
    public static native String getSystemInfo();
}
