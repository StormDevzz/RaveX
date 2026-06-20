package ravex.addon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddonManager {
    public static final AddonManager INSTANCE = new AddonManager();
    private final List<Addon> loadedAddons = new ArrayList<>();
    private final AddonRegistry registry = new AddonRegistry();

    private AddonManager() {}

    private native void nativeInitAddons(String dirPath);
    private native void nativeUnloadAddons();

    public void init() {
        ravex.RaveX.LOGGER.info("[AddonManager] Initializing addon system...");
        File addonsDir = new File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "ravex/addons");
        if (!addonsDir.exists()) {
            addonsDir.mkdirs();
        }

        // Load native C++ addons
        File nativeAddonsDir = new File(addonsDir, "native");
        if (!nativeAddonsDir.exists()) {
            nativeAddonsDir.mkdirs();
        }
        try {
            nativeInitAddons(nativeAddonsDir.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            ravex.RaveX.LOGGER.warn("[AddonManager] Native addon JNI not linked: " + e.getMessage());
        }

        File[] files = addonsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (files != null) {
            for (File f : files) {
                try {
                    Addon addon = AddonLoader.loadAddon(f);
                    AddonContext context = new AddonContext(addon.getInfo());
                    addon.onLoad(context);
                    loadedAddons.add(addon);
                    registry.register(addon);
                    ravex.RaveX.LOGGER.info("[AddonManager] Successfully loaded addon: " + addon.getInfo().getName() + " v" + addon.getInfo().getVersion());
                } catch (Exception e) {
                    ravex.RaveX.LOGGER.error("[AddonManager] Failed to load addon from " + f.getName() + ": " + e.getMessage());
                }
            }
        }
    }

    public List<Addon> getLoadedAddons() { return loadedAddons; }
    public AddonRegistry getRegistry() { return registry; }

    public void shutdown() {
        try {
            nativeUnloadAddons();
        } catch (UnsatisfiedLinkError ignored) {}
        for (Addon a : loadedAddons) {
            a.onUnload();
        }
        loadedAddons.clear();
    }
}
