package ravex.gui.descriptions;

import java.util.HashMap;
import java.util.Map;

public class HudDescriptions {
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        DESCRIPTIONS.put("Watermark", "Renders the premium RaveX Client watermark on screen.");
        DESCRIPTIONS.put("ArrayList", "Renders the active (enabled) modules array list.");
        DESCRIPTIONS.put("Coords", "Renders player's current X, Y, Z coordinates.");
        DESCRIPTIONS.put("Fps", "Renders the current game frames per second (FPS) count.");
        DESCRIPTIONS.put("Media", "Shows currently playing music from your system.");
        DESCRIPTIONS.put("Indicators", "Arc gauges for Health, Armor, TPS, Speed, and Knockback.");
        DESCRIPTIONS.put("InvPreview", "Preview of your full inventory and selected hotbar slot.");
        DESCRIPTIONS.put("Chat", "Repositions and scales the in-game chat box.");
        DESCRIPTIONS.put("TPS", "Current server tick rate (TPS) monitor.");
        DESCRIPTIONS.put("Cooldowns", "Displays item cooldowns (Ender Pearl, Chorus, etc.) with remaining percentage.");
        DESCRIPTIONS.put("Currency", "Renders custom live/simulated currency exchange rates for CIS & NATO countries + BTC.");
        DESCRIPTIONS.put("ServerBrand", "Displays the server implementation brand using C++ JNI formatting.");
        DESCRIPTIONS.put("TargetHud", "Displays target player statistics, health, armor, and hand items.");
        DESCRIPTIONS.put("BindListHud", "List of the modules that have a keybind.");
        DESCRIPTIONS.put("ArmorHud", "Modern armor pieces with durability bars.");
        DESCRIPTIONS.put("ChestCounterHud", "Counts nearby single and double chests.");
        DESCRIPTIONS.put("RamHud", "Memory usage bar with history graph.");
        DESCRIPTIONS.put("StaffBoardHud", "Detects staff and admins on popular servers.");
        DESCRIPTIONS.put("HotbarHud", "Replaces the vanilla hotbar with a clean modern one.");
        DESCRIPTIONS.put("Radar", "Minimap with entity dots.");
    }

    public static String getDescription(String moduleName) {
        String desc = DESCRIPTIONS.get(moduleName);
        if (desc == null && moduleName.endsWith("Hud")) {
            desc = DESCRIPTIONS.get(moduleName.substring(0, moduleName.length() - 3));
        }
        return desc != null ? desc : "No HUD module description available.";
    }
}
