package ravex.addon.template;

import ravex.addon.Addon;
import ravex.addon.AddonContext;
import ravex.addon.AddonInfo;

public class AnotherAddon implements Addon {
    private final AddonInfo info = new AddonInfo(
        "AnotherAddon",
        "A template example addon that adds Custom category and Another module",
        "1.0.0",
        "RaveXDeveloper",
        "ravex.addon.template.AnotherAddon"
    );

    private AnotherModule module;

    @Override
    public void onLoad(AddonContext context) {
        context.getLogger().info("AnotherAddon loading...");
        this.module = new AnotherModule(this);
        context.registerModule(this.module);
        context.getLogger().info("AnotherAddon loaded successfully and registered module 'Another' in Category.CUSTOM!");
    }

    @Override
    public void onUnload() {
        // Cleanup resources if any
    }

    @Override
    public AddonInfo getInfo() {
        return info;
    }
}
