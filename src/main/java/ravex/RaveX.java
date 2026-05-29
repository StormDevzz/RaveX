package ravex;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ravex.modules.ModuleManager;
import ravex.utility.misc.GithubUtility;
import net.minecraft.client.Minecraft;

public class RaveX implements ClientModInitializer {
    public static final String MOD_ID = "ravex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final String version = FabricLoader.getInstance()
        .getModContainer(MOD_ID)
        .orElseThrow()
        .getMetadata()
        .getVersion()
        .getFriendlyString();

    private static boolean rightShiftWasDown = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("===== Initializing RaveX client v" + version + " =====");

        // 1. Initialize ModuleManager
        LOGGER.info("Initializing ModuleManager...");
        ModuleManager.INSTANCE.init();
        LOGGER.info("Successfully registered " + ModuleManager.INSTANCE.getModules().size() + " modules!");

        // 2. Load macros
        LOGGER.info("Loading macros...");
        ravex.macro.MacroManager.INSTANCE.load();
        LOGGER.info("Loaded " + ravex.macro.MacroManager.INSTANCE.getMacros().size() + " macros!");

        // 3. Load profiles
        LOGGER.info("Loading profiles...");
        ravex.profile.ProfileManager.INSTANCE.load();
        LOGGER.info("Loaded " + ravex.profile.ProfileManager.INSTANCE.getProfiles().size() + " profiles!");

        LOGGER.info("Connecting to Github API...");
        new Thread(GithubUtility::checkUpdates).start();

        LOGGER.info("===== RaveX successfully initialized! =====");
    }

    public static void onClientTick() {
        ModuleManager.INSTANCE.onTick();
        ravex.macro.MacroManager.INSTANCE.onTick();
        ravex.utility.lua.LuaManager.INSTANCE.onTick(); // fire Lua timers (e.g. RichPresence)

        // Direct, bulletproof GLFW right shift key press checking!
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.getWindow() != null) {
            com.mojang.blaze3d.platform.Window window = mc.getWindow();
            boolean isDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
            
            if (isDown && !rightShiftWasDown) {
                // Key pressed event!
                if (mc.screen == null) {
                    mc.setScreen(new ravex.gui.clickgui.ClickGUI());
                } else if (mc.screen instanceof ravex.gui.clickgui.ClickGUI) {
                    mc.setScreen(null);
                }
            }
            rightShiftWasDown = isDown;
        }
    }
}
