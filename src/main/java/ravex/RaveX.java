package ravex;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ravex.manager.ModuleManager;
import ravex.utility.misc.GithubUtility;
import ravex.utility.nativelib.NativeLibrary;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

public class RaveX implements ModInitializer, ClientModInitializer, PreLaunchEntrypoint {
    public static final String MOD_ID = "ravex";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final String version = FabricLoader.getInstance()
        .getModContainer(MOD_ID)
        .orElseThrow()
        .getMetadata()
        .getVersion()
        .getFriendlyString();

    private static final NativeLibrary NATIVE = NativeLibrary.of("ravex_addon");

    static {
        NATIVE.load();
    }

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

    @Override
    public void onInitialize() {
        ravex.utility.sound.SoundUtility.register();
    }

    @Override
    public void onPreLaunch() {
        try {
            java.io.File signal = new java.io.File(System.getProperty("java.io.tmpdir"), ".ravex_ready");
            if (signal.exists()) signal.delete();
        } catch (Exception ignored) {}

        if (!"true".equals(System.getenv("RAVEX_LOADER_ACTIVE"))) {
            try {
                String jarPath = getModJarPath();
                if (jarPath != null && !jarPath.isEmpty()) {
                    String javaExe = System.getProperty("java.home") + "/bin/java";
                    java.io.File exeFile = new java.io.File(javaExe);
                    if (!exeFile.exists()) javaExe = "java";

                    java.util.List<String> cmd = new java.util.ArrayList<>();
                    cmd.add(javaExe);
                    cmd.add("-Djava.awt.headless=false");
                    cmd.add("-cp");
                    cmd.add(jarPath);
                    cmd.add("ravex.loader.RaveXLoader");
                    cmd.add("--integrated-gui");

                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    setLoaderProcess(p);

                    new Thread(() -> {
                        try (java.io.BufferedReader r = new java.io.BufferedReader(
                                new java.io.InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = r.readLine()) != null) {
                                LOGGER.info("[RaveX-Loader] " + line);
                            }
                        } catch (Exception ignored) {}
                    }).start();
                }
            } catch (Exception ignored) {}
        }
    }

    private static final boolean[] keysState = new boolean[512];

    @Override
    public void onInitializeClient() {
        String osName = ravex.loader.RaveXLoader.getDetailedOSName();
        LOGGER.info("RaveX v{} on {}", version, osName);

        ravex.manager.NativeManager.INSTANCE.check();
        ravex.utility.misc.GuiOptimizer.optimize();

        ModuleManager.INSTANCE.init();
        LOGGER.info("Registered {} modules", ModuleManager.INSTANCE.getModules().size());

        try {
            ravex.manager.AddonManager.INSTANCE.init();
        } catch (Exception e) {
            LOGGER.error("AddonManager init failed", e);
        }

        ravex.manager.MacroManager.INSTANCE.load();
        int mc = ravex.manager.MacroManager.INSTANCE.getMacros().size();
        if (mc > 0) LOGGER.info("Loaded {} macro(s)", mc);

        ravex.manager.ProfileManager.INSTANCE.load();
        int pc = ravex.manager.ProfileManager.INSTANCE.getProfiles().size();
        if (pc > 0) LOGGER.info("Loaded {} profile(s)", pc);

        try {
            ravex.manager.ConfigManager.INSTANCE.load("default");
        } catch (Exception e) {
            LOGGER.error("Failed to load default config", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                ravex.manager.AddonManager.INSTANCE.shutdown();
            } catch (Throwable ignored) {}
            ravex.manager.ConfigManager.INSTANCE.save("default");
        }));

        new Thread(ravex.utility.misc.GithubUtility::checkUpdates).start();
    }

    public static void onClientTick() {
        if (!loaderProcessClosed) {
            closeLoaderProcess();
            createReadySignal();
            loaderProcessClosed = true;
        }

        ModuleManager.INSTANCE.onTick();
        ravex.manager.MacroManager.INSTANCE.onTick();
        ravex.manager.LuaManager.INSTANCE.onTick(); 

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
