package ravex.descriptions;

import java.util.HashMap;
import java.util.Map;

public class HudDescriptions {
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        DESCRIPTIONS.put("Watermark", "Renders the premium RaveX Client watermark on screen.");
        DESCRIPTIONS.put("ActiveModules", "Renders the active (enabled) modules array list.");
        DESCRIPTIONS.put("Coords", "Renders player's current X, Y, Z coordinates.");
        DESCRIPTIONS.put("Fps", "Renders the current game frames per second (FPS) count.");
    }

    public static String getDescription(String moduleName) {
        return DESCRIPTIONS.getOrDefault(moduleName, "No HUD module description available.");
    }
}
