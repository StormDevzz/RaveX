package ravex.descriptions;

import java.util.HashMap;
import java.util.Map;

public class ClickGuiDescriptions {
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        DESCRIPTIONS.put("KillAura",      "Auto-attacks nearby entities.");
        DESCRIPTIONS.put("ESP",           "See entities through walls.");
        DESCRIPTIONS.put("AutoTool",      "Picks best tool automatically.");
        DESCRIPTIONS.put("AntiAfk",       "Prevents AFK kick.");
        DESCRIPTIONS.put("BoneMeal",      "Auto-fertilizes nearby crops.");
        DESCRIPTIONS.put("ClickGui",      "GUI appearance settings.");
        DESCRIPTIONS.put("Notifications", "Module toggle alerts.");
        DESCRIPTIONS.put("VisualRange",   "Alerts when players enter range.");
        DESCRIPTIONS.put("NoBob",         "Disables view bobbing.");
        DESCRIPTIONS.put("Ambient",       "Overlay screen tint color.");
        DESCRIPTIONS.put("CustomFog",     "Custom fog color.");
        DESCRIPTIONS.put("AimAssist",     "Smooth aim toward targets.");
        DESCRIPTIONS.put("NameTags",      "Custom entity nametags.");
        DESCRIPTIONS.put("Trigger",       "Auto-attacks on crosshair.");
        DESCRIPTIONS.put("MaceSwap",      "Swaps to mace when falling.");
        DESCRIPTIONS.put("Hud",           "HUD elements & layout editor.");
        DESCRIPTIONS.put("Optimizer",     "Memory optimization (JVM + native).");
        DESCRIPTIONS.put("NoWeb",         "Negates cobweb slowdown.");
        DESCRIPTIONS.put("Glint",         "Custom enchantment glint color.");
        DESCRIPTIONS.put("RichPresence",  "Discord Rich Presence status.");
        DESCRIPTIONS.put("GuiWalk",       "Walk while a GUI is open.");
        DESCRIPTIONS.put("NoSlowDown",    "No item-use slowdown (Vanilla/Grim/NCP).");
        DESCRIPTIONS.put("Velocity",      "Reduce received knockback.");
        DESCRIPTIONS.put("AutoEat",       "Auto-eats when hunger is low.");
        DESCRIPTIONS.put("NoInteract",    "Prevents accidental container interaction.");
    }

    public static String getDescription(String moduleName) {
        return DESCRIPTIONS.getOrDefault(moduleName, "No description.");
    }
}
