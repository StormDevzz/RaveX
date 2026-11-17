package ravex.cmd.cmds;

import ravex.utility.nativelib.NativeLibraryUtility;

/**
 * Bridge to the Rust core library (libravex_core.so / ravex_core.dll).
 * Implements the calc expression evaluator natively.
 * If the library is unavailable, callers fall back to the Java implementation in CalcUtility.
 */
public final class CalcNative {
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_core");

    static {
        NATIVE.load();
    }

    private CalcNative() {
    }

    public static boolean isAvailable() {
        return NATIVE.isLoaded() || NATIVE.load();
    }

    /**
     * Evaluates a math expression.
     *
     * @return true on success (value written to out[0], may be NaN/Infinity),
     *         false on parse error (the Java fallback throws there).
     */
    public static native boolean nativeEval(String expr, double[] out);
}
