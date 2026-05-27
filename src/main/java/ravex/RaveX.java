package ravex;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ravex.modules.ModuleManager;
import ravex.utility.misc.GithubUtility;
import ravex.utility.sound.SoundUtility;
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

        // 2. Register custom SoundEvents safely by unfreezing MappedRegistry
        LOGGER.info("Registering custom SoundEvents...");
        SoundUtility.register();

        // 3. Github integration (not a module)
        LOGGER.info("Connecting to Github API...");
        new Thread(GithubUtility::checkUpdates).start();

        LOGGER.info("===== RaveX successfully initialized! =====");
    }

    public static void onClientTick() {
        ModuleManager.INSTANCE.onTick();
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
