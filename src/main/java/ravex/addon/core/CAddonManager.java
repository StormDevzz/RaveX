package ravex.addon.core;

import ravex.RaveX;
import ravex.addon.security.AddonSignature;
import ravex.addon.util.AddonException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CAddonManager {
    private static boolean nativeLoaded = false;
    private static boolean initialized = false;

    private static native void nativeInitCAddons(String[] paths);
    private static native void nativeUnloadCAddons();
    private static native void nativeTickCAddons();
    private static native void nativeKeyEventCAddons(int key, int action);

    public static void init() {
        if (initialized) return;
        initialized = true;

        if (!tryLoadNative()) return;

        File addonsDir = new File(
            net.minecraft.client.Minecraft.getInstance().gameDirectory,
            "RaveX/addons/c_native"
        );
        if (!addonsDir.exists()) addonsDir.mkdirs();

        File[] files = addonsDir.listFiles((dir, name) ->
            name.startsWith("c_addon_") && name.endsWith(".so"));
        if (files == null || files.length == 0) return;

        List<String> verified = new ArrayList<>();
        for (File f : files) {
            try {
                AddonSignature.requireSignature(f.toPath());
                verified.add(f.getAbsolutePath());
            } catch (AddonException e) {
                RaveX.LOGGER.warn("[CAddonManager] Skipping unsigned addon: {} - {}",
                    f.getName(), e.getMessage());
            }
        }

        if (verified.isEmpty()) return;

        try {
            nativeInitCAddons(verified.toArray(new String[0]));
            RaveX.LOGGER.info("[CAddonManager] Loaded {} C addon(s)", verified.size());
        } catch (UnsatisfiedLinkError e) {
            RaveX.LOGGER.warn("[CAddonManager] Failed to init C addons: {}", e.getMessage());
        }
    }

    public static void shutdown() {
        if (!initialized) return;
        try {
            nativeUnloadCAddons();
        } catch (UnsatisfiedLinkError ignored) {}
    }

    public static void onTick() {
        if (!initialized) return;
        try {
            nativeTickCAddons();
        } catch (UnsatisfiedLinkError ignored) {}
    }

    public static void onKeyEvent(int key, int action) {
        if (!initialized) return;
        try {
            nativeKeyEventCAddons(key, action);
        } catch (UnsatisfiedLinkError ignored) {}
    }

    private static boolean tryLoadNative() {
        if (nativeLoaded) return true;

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return false;

        String libName = "ravex_c_addon_loader";
        try {
            System.loadLibrary(libName);
            nativeLoaded = true;
            return true;
        } catch (UnsatisfiedLinkError e) {
            try {
                ravex.utility.nativelib.NativeLoader.loadLibrary(libName);
                nativeLoaded = true;
                return true;
            } catch (Exception ignored) {}
        }

        RaveX.LOGGER.warn("[CAddonManager] Native library {} not available", libName);
        return false;
    }
}
