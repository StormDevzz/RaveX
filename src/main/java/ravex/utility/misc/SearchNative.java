package ravex.utility.misc;

import ravex.utility.nativelib.NativeLibraryUtility;

/**
 * Bridge to the Rust core library (libravex_core.so / ravex_core.dll).
 * Implements the search logic (translit, keyboard layouts, diacritics) natively.
 * If the library is unavailable, callers fall back to the Java implementation in SearchUtility.
 */
public final class SearchNative {
    private static final NativeLibraryUtility NATIVE = NativeLibraryUtility.of("ravex_core");

    static {
        NATIVE.load();
    }

    private SearchNative() {
    }

    public static boolean isAvailable() {
        return NATIVE.isLoaded() || NATIVE.load();
    }

    public static native String nativeLower(String s);

    public static native String nativeStripDiacritics(String s);

    public static native String nativeMapKeys(String s, boolean ruToEn);

    public static native String nativeTranslit(String s);

    public static native String nativeSwapShaScha(String s);

    public static native String[] nativeQueryVariants(String query);
}
