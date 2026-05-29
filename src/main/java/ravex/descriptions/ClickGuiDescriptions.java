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
        DESCRIPTIONS.put("VisualRange",   "Player radar logs.");
        DESCRIPTIONS.put("NoBob",         "Removes view bobbing.");
        DESCRIPTIONS.put("Ambient",       "Custom time & lighting.");
        DESCRIPTIONS.put("CustomFog",     "Adjust fog visual.");
        DESCRIPTIONS.put("AimAssist",     "Smooth aim helper.");
        DESCRIPTIONS.put("NameTags",      "Custom wallhack nametags.");
        DESCRIPTIONS.put("Trigger",       "Auto swing on target.");
        DESCRIPTIONS.put("MaceSwap",      "Instantly swap to mace.");
        DESCRIPTIONS.put("Hud",           "Edit HUD overlay.");
        DESCRIPTIONS.put("Optimizer",     "Boost performance.");
        DESCRIPTIONS.put("NoWeb",         "Ignore cobweb slow.");
        DESCRIPTIONS.put("Glint",         "Enchantment color.");
        DESCRIPTIONS.put("RichPresence",  "Discord status.");
        DESCRIPTIONS.put("GuiWalk",       "Walk in interfaces.");
        DESCRIPTIONS.put("NoSlowDown",    "Item use speedup.");
        DESCRIPTIONS.put("Velocity",      "Cancel knockback.");
        DESCRIPTIONS.put("AutoEat",       "Auto eat food.");
        DESCRIPTIONS.put("NoInteract",    "Block container open.");
        DESCRIPTIONS.put("SourceFiller",  "Quick fluid drain.");
        DESCRIPTIONS.put("AirPlace",      "Place blocks in air.");
        DESCRIPTIONS.put("Scaffold",      "Auto bridge under feet.");
        DESCRIPTIONS.put("Shaders",       "Volumetric visual waves.");
        DESCRIPTIONS.put("FreeLook",      "Rotate camera freely.");
        DESCRIPTIONS.put("FreeCam",       "Body spectator fly.");
        DESCRIPTIONS.put("ViewClip",      "Clip camera through blocks.");
        DESCRIPTIONS.put("Step",          "Step up full blocks.");
        DESCRIPTIONS.put("ReverseStep",   "Fast pull down.");
        DESCRIPTIONS.put("Spammer",       "Spam chat from text or file.");
        DESCRIPTIONS.put("Commands",      "Client command processor.");
    }

    public static String getDescription(String moduleName) {
        return DESCRIPTIONS.getOrDefault(moduleName, "No description.");
    }
}
