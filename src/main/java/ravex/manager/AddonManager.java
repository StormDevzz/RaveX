package ravex.manager;

import ravex.addon.Addon;
import ravex.addon.core.AddonContext;
import ravex.addon.core.AddonLoader;
import ravex.addon.module.AddonRegistry;
import ravex.addon.security.AddonSignature;
import ravex.addon.util.AddonException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddonManager {
    public static final AddonManager INSTANCE = new AddonManager();
    private final List<Addon> loadedAddons = new ArrayList<>();
    private final AddonRegistry registry = new AddonRegistry();

    private AddonManager() {}

    private native void nativeInitAddons(String[] paths);
    private native void nativeUnloadAddons();

    public void init() {
        ravex.addon.core.CAddonManager.init();
        File addonsDir = new File(net.minecraft.client.Minecraft.getInstance().gameDirectory, "RaveX/addons");
        if (!addonsDir.exists()) addonsDir.mkdirs();

        File nativeAddonsDir = new File(addonsDir, "native");
        if (!nativeAddonsDir.exists()) nativeAddonsDir.mkdirs();

        File[] nativeFiles = nativeAddonsDir.listFiles((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".so") || lower.endsWith(".dll");
        });

        List<String> verifiedNative = new ArrayList<>();
        if (nativeFiles != null) {
            for (File f : nativeFiles) {
                try {
                    AddonSignature.requireSignature(f.toPath());
                    verifiedNative.add(f.getAbsolutePath());
                } catch (AddonException e) {
                    ravex.RaveX.LOGGER.warn("[AddonManager] Skipping unsigned native addon: {} - {}",
                        f.getName(), e.getMessage());
                }
            }
        }

        if (!verifiedNative.isEmpty()) {
            try {
                nativeInitAddons(verifiedNative.toArray(new String[0]));
            } catch (UnsatisfiedLinkError ignored) {}
        }

        File[] jarFiles = addonsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        int count = 0;
        if (jarFiles != null) {
            for (File f : jarFiles) {
                try {
                    Addon addon = AddonLoader.loadAddon(f);
                    AddonContext context = new AddonContext(addon.getInfo());
                    addon.onLoad(context);
                    loadedAddons.add(addon);
                    registry.register(addon);
                    count++;
                } catch (Exception e) {
                    ravex.RaveX.LOGGER.error("[AddonManager] Failed to load addon from {}: {}", f.getName(), e.getMessage());
                }
            }
        }
        ravex.RaveX.LOGGER.info("[AddonManager] Loaded {} Java addon(s), {} native addon(s)", count, verifiedNative.size());
    }

    public List<Addon> getLoadedAddons() { return loadedAddons; }
    public AddonRegistry getRegistry() { return registry; }

    public void shutdown() {
        ravex.addon.core.CAddonManager.shutdown();
        try {
            nativeUnloadAddons();
        } catch (UnsatisfiedLinkError ignored) {}
        for (Addon a : loadedAddons) {
            a.onUnload();
        }
        loadedAddons.clear();
    }
}
