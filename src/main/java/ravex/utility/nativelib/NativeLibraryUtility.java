package ravex.utility.nativelib;

import ravex.utility.nativelib.NativeLoader;

public class NativeLibraryUtility {
    private static volatile boolean loadingEnabled = false;

    public static void deferLoading() {
        loadingEnabled = false;
    }

    public static void enableLoading() {
        loadingEnabled = true;
    }

    private final String name;
    private boolean loaded;

    public NativeLibraryUtility(String libName) {
        this.name = normalizeName(libName);
    }

    public NativeLibraryUtility() {
        this.name = inferLibName();
    }

    public static NativeLibraryUtility of(String libName) {
        return new NativeLibraryUtility(libName);
    }

    private static String normalizeName(String name) {
        if (name.startsWith("lib") || name.endsWith(".dll") || name.endsWith(".so"))
            return name;
        if (name.startsWith("ravex_"))
            return name;
        return "ravex_" + name;
    }

    private String inferLibName() {
        String cls = getClass().getSimpleName();
        StringBuilder sb = new StringBuilder("ravex_");
        for (int i = 0; i < cls.length(); i++) {
            char c = cls.charAt(i);
            if (Character.isUpperCase(c) && sb.length() > 7)
                sb.append('_').append(Character.toLowerCase(c));
            else
                sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    public boolean load() {
        if (loaded) return true;
        if (!loadingEnabled) return false;
        loaded = NativeLoader.loadLibrary(name);
        return loaded;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getName() {
        return name;
    }

    public void require() {
        if (!loaded) {
            loaded = NativeLoader.loadLibrary(name);
        }
        if (!loaded) throw new NativeException("Native library '" + name + "' is not loaded. Call load() first.");
    }

    static {
        NativeLoader.load();
    }
}
