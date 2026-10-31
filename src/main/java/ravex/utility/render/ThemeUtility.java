package ravex.utility.render;

import ravex.manager.ModuleManager;

// preset accent palettes.
// a preset repaints all accent surfaces at once
public class ThemeUtility {

    // curated accents shown as theme presets in the colour palette
    public static final int[] PRESETS = {
        0xFF40A9F8, // ravex default
        0xFF3B82F6, // azure
        0xFF6366F1, // indigo
        0xFF8B5CF6, // violet
        0xFFA78BFA, // lavender
        0xFFEC4899, // pink
        0xFFF43F5E, // rose
        0xFFFF7A59, // coral
        0xFFF59E0B, // amber
        0xFFEAB308, // gold
        0xFF84CC16, // lime
        0xFF10B981, // emerald
        0xFF14B8A6, // teal
        0xFF2DD4BF, // mint
        0xFF38BDF8, // sky
        0xFF94A3B8  // slate
    };

    private ThemeUtility() {}

    // paints one accent over the gui, menu and notification colours
    public static void apply(int accent) {
        ravex.modules.client.ClickGui cfg = ModuleManager.get(ravex.modules.client.ClickGui.class);
        if (cfg != null) {
            cfg.colorMode = "Static";
            cfg.color1 = accent;
        }
        ravex.modules.client.Settings settings = ModuleManager.get(ravex.modules.client.Settings.class);
        if (settings != null) {
            settings.menuColor = accent;
        }
        ravex.modules.client.Notifications notifications = ModuleManager.get(ravex.modules.client.Notifications.class);
        if (notifications != null) {
            notifications.messageColor = accent;
        }
        ColorUtility.invalidateColorCache();
    }

    // true when the given accent is the one currently in use
    public static boolean isCurrent(int accent) {
        ravex.modules.client.ClickGui cfg = ModuleManager.get(ravex.modules.client.ClickGui.class);
        if (cfg == null) return false;
        if (!"Static".equals(cfg.colorMode) || cfg.color1 != accent) return false;
        ravex.modules.client.Settings settings = ModuleManager.get(ravex.modules.client.Settings.class);
        return settings == null || settings.menuColor == accent;
    }
}
