package ravex.addon.template;

import ravex.addon.Addon;
import ravex.addon.core.AddonContext;
import ravex.addon.core.AddonInfo;

public class MainAddon implements Addon {
    private AddonContext context;

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    @Override
    public void onLoad(AddonContext context) {
        this.context = context;
        context.getLogger().info("MainAddon loading on " + (isWindows() ? "Windows" : "Linux"));
        context.registerModule(new DemoModule(this));
        context.getLogger().info("MainAddon loaded");
    }

    @Override
    public void onUnload() {
        context.getLogger().info("MainAddon unloading");
    }

    @Override
    public AddonInfo getInfo() {
        return new AddonInfo(
            "MainAddon",
            "Minimal RaveX Java addon",
            "1.0.0",
            "RaveX Team",
            "ravex.addon.template.MainAddon"
        );
    }
}
