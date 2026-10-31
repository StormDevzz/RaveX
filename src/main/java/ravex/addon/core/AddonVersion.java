package ravex.addon.core;

// api version the client offers to addons.
public final class AddonVersion {
    public static final String API_VERSION = "1.5";

    private static final int API_MAJOR = 1;
    private static final int API_MINOR = 5;

    private AddonVersion() {
    }

        // addons built for an older client keep loading.
    // only addons that need a newer api are rejected
    public static boolean isCompatible(String addonVersion) {
        if (addonVersion == null) return true;
        String cleaned = addonVersion.trim();
        if (cleaned.startsWith("v") || cleaned.startsWith("V")) cleaned = cleaned.substring(1);
        int dot = cleaned.indexOf('.');
        if (dot <= 0) return true;
        int major = parse(cleaned.substring(0, dot));
        if (major < 0) return true;
        int minor = parse(partAfter(cleaned, dot));
        if (minor < 0) return true;
        if (major != API_MAJOR) return major < API_MAJOR;
        return minor <= API_MINOR;
    }

    private static String partAfter(String value, int dot) {
        int end = value.indexOf('.', dot + 1);
        return end < 0 ? value.substring(dot + 1) : value.substring(dot + 1, end);
    }

    private static int parse(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
