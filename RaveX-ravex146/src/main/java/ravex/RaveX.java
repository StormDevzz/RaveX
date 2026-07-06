package ravex;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ravex.modules.ModuleManager;
import ravex.utility.misc.GithubUtility;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

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
    private static Process loaderProcess = null;
    private static boolean loaderProcessClosed = false;

    public static void closeLoaderProcess() {
        if (loaderProcess != null) {
            try {
                LOGGER.info("[RaveX] Game fully loaded! Closing loader process...");
                loaderProcess.destroy();
                loaderProcess = null;
            } catch (Exception e) {
                LOGGER.error("Failed to destroy loader process", e);
            }
        }
    }

    public static void createReadySignal() {
        try {
            java.io.File signal = new java.io.File(System.getProperty("java.io.tmpdir"), ".ravex_ready");
            signal.createNewFile();
            LOGGER.info("[RaveX] Created ready signal file: " + signal.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Failed to create ready signal file", e);
        }
    }

    public static String getModJarPath() {
        
        try {
            java.util.Optional<net.fabricmc.loader.api.ModContainer> container = 
                net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("ravex");
            if (container.isPresent()) {
                java.util.List<java.nio.file.Path> paths = container.get().getOrigin().getPaths();
                if (!paths.isEmpty()) {
                    java.io.File file = paths.get(0).toFile();
                    if (file.exists() && file.isFile() && file.getName().endsWith(".jar")) {
                        return file.getAbsolutePath();
                    }
                }
            }
        } catch (Exception ignored) {}

        
        try {
            java.io.File file = new java.io.File(
                ravex.loader.RaveXLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()
            );
            if (file.exists() && file.isFile() && file.getName().endsWith(".jar")) {
                return file.getAbsolutePath();
            }
        } catch (Exception ignored) {}

        
        try {
            java.io.File modsDir = new java.io.File(
                net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().toFile(),
                "mods"
            );
            if (modsDir.exists() && modsDir.isDirectory()) {
                java.io.File[] files = modsDir.listFiles();
                if (files != null) {
                    for (java.io.File f : files) {
                        if (f.isFile() && f.getName().toLowerCase().contains("ravex") && f.getName().endsWith(".jar")) {
                            return f.getAbsolutePath();
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return "";
    }

    public static void setLoaderProcess(Process p) {
        loaderProcess = p;
    }

    private static final boolean[] keysState = new boolean[512];

    @Override
    public void onInitializeClient() {
        LOGGER.info("===== Initializing RaveX client v" + version + " =====");
        
        
        String osName = ravex.loader.RaveXLoader.getDetailedOSName();
        System.out.println("\u001B[35m┌────────────────────────────────────────────────────────┐\u001B[0m");
        System.out.println("\u001B[35m│\u001B[36m   RaveX Client loaded on Operating System:              \u001B[35m│\u001B[0m");
        System.out.printf("\u001B[35m│\u001B[32m   %-53s\u001B[35m│\u001B[0m\n", osName);
        System.out.println("\u001B[35m└────────────────────────────────────────────────────────┘\u001B[0m");

        LOGGER.info("[RaveX] Operating System: " + osName);

        
        ravex.manager.NativeManager.INSTANCE.check();

        
        ravex.utility.misc.GuiOptimizer.optimize();

        
        LOGGER.info("Initializing ModuleManager...");
        ModuleManager.INSTANCE.init();
        LOGGER.info("Successfully registered " + ModuleManager.INSTANCE.getModules().size() + " modules!");

        
        try {
            ravex.utility.misc.NativeLoader.loadLibrary("ravex_addon");
        } catch (UnsatisfiedLinkError e) {
            LOGGER.error("[Addon System] Native library ravex_addon load failed", e);
        }
        try {
            LOGGER.info("Initializing AddonManager...");
            ravex.addon.AddonManager.INSTANCE.init();
        } catch (Exception e) {
            LOGGER.error("[Addon System] Failed to initialize AddonManager", e);
        }

        
        LOGGER.info("Loading macros...");
        ravex.macro.MacroManager.INSTANCE.load();
        LOGGER.info("Loaded " + ravex.macro.MacroManager.INSTANCE.getMacros().size() + " macros!");

        
        LOGGER.info("Loading profiles...");
        ravex.profile.ProfileManager.INSTANCE.load();
        LOGGER.info("Loaded " + ravex.profile.ProfileManager.INSTANCE.getProfiles().size() + " profiles!");

        
        try {
            LOGGER.info("[RaveX] Loading default configuration...");
            ravex.manager.ConfigManager.INSTANCE.load("default");
        } catch (Exception e) {
            LOGGER.error("Failed to load default config", e);
        }

        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("[RaveX] Game shutting down! Automatically saving default configuration...");
            try {
                ravex.addon.AddonManager.INSTANCE.shutdown();
            } catch (Throwable ignored) {}
            ravex.manager.ConfigManager.INSTANCE.save("default");
        }));

        LOGGER.info("Connecting to Github API...");
        new Thread(ravex.utility.misc.GithubUtility::checkUpdates).start();

        System.out.println("[RaveX-Java] Loading complete");
        LOGGER.info("===== RaveX successfully initialized! =====");
    }

    public static void onClientTick() {
        if (!loaderProcessClosed) {
            closeLoaderProcess();
            createReadySignal();
            loaderProcessClosed = true;
        }

        ModuleManager.INSTANCE.onTick();
        ravex.macro.MacroManager.INSTANCE.onTick();
        ravex.utility.lua.LuaManager.INSTANCE.onTick(); 

        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        com.mojang.blaze3d.platform.Window window = mc.getWindow();

        
        boolean isDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (isDown && !rightShiftWasDown) {
            if (mc.screen instanceof ravex.gui.clickgui.ClickGUI) {
                mc.setScreen(null);
            } else {
                mc.setScreen(new ravex.gui.clickgui.ClickGUI());
            }
        }
        rightShiftWasDown = isDown;

        
        for (ravex.modules.Module m : ModuleManager.INSTANCE.getModules()) {
            int bind = m.getKeyBind();
            if (bind > 0 && bind < keysState.length) {
                boolean isKeyBindDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, bind);
                if (isKeyBindDown && !keysState[bind]) {
                    if (mc.screen == null || mc.screen instanceof ravex.gui.clickgui.ClickGUI) {
                        m.toggle();
                    }
                }
                keysState[bind] = isKeyBindDown;
            }
        }
    }
}
