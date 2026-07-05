package ravex.utility.nativelib;

import ravex.utility.nativelib.NativeLoader;

public class NativeLibrary {
    private final String name;
    private boolean loaded;

    public NativeLibrary(String libName) {
        this.name = normalizeName(libName);
    }

    public NativeLibrary() {
        this.name = inferLibName();
    }

    public static NativeLibrary of(String libName) {
        return new NativeLibrary(libName);
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
        if (!loaded) throw new NativeException("Native library '" + name + "' is not loaded. Call load() first.");
    }

    static {
        NativeLoader.load();
    }
}
