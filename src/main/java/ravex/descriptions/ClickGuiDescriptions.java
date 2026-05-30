package ravex.descriptions;

import java.util.HashMap;
import java.util.Map;

public class ClickGuiDescriptions {
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();

    static {
        DESCRIPTIONS.put("KillAura",      "Auto attacks targets.");
        DESCRIPTIONS.put("ESP",           "Render wallhack overlays.");
        DESCRIPTIONS.put("AutoTool",      "Picks the best tool.");
        DESCRIPTIONS.put("AntiAfk",       "Prevents being kicked.");
        DESCRIPTIONS.put("BoneMeal",      "Fertilizes crops.");
        DESCRIPTIONS.put("ClickGui",      "Configure GUI look.");
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
        DESCRIPTIONS.put("SourceFiller",  "Automatically places sponges to dry nearby water.");
        DESCRIPTIONS.put("AirPlace",      "Place blocks in the air smoothly.");
        DESCRIPTIONS.put("Scaffold",      "Vanilla-like helper to automatically place blocks under your feet.");
        DESCRIPTIONS.put("Shaders",       "Volumetric visual waves.");
        DESCRIPTIONS.put("FreeLook",      "Rotate camera freely.");
        DESCRIPTIONS.put("FreeCam",       "Body spectator fly.");
        DESCRIPTIONS.put("ViewClip",      "Clip camera through blocks.");
        DESCRIPTIONS.put("Step",          "Step up full blocks.");
        DESCRIPTIONS.put("ReverseStep",   "Fast pull down.");
        DESCRIPTIONS.put("Spammer",       "Spam chat from text or file.");
        DESCRIPTIONS.put("Commands",      "Client command processor.");
        DESCRIPTIONS.put("ItemPhysics",   "Realistic physical rotation and flat rendering for dropped items.");
    }

    public static String getDescription(String moduleName) {
        return DESCRIPTIONS.getOrDefault(moduleName, "No description.");
    }
}
