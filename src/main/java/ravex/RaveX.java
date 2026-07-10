package ravex;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<<<<<<< HEAD
import ravex.di.Injector;
import ravex.di.ServiceLocator;
import ravex.event.EventBusHolder;
import ravex.event.Subscribe;
import ravex.event.client.ScreenEvent;
import ravex.event.client.TickEvent;
import ravex.event.combat.ModuleToggleEvent;
import ravex.event.network.PacketEvent;
import ravex.event.render.CameraEvent;
import ravex.event.player.DeathEvent;
import ravex.event.movement.VelocityEvent;
import ravex.event.render.FogEvent;
import ravex.event.combat.AttackEvent;
import ravex.event.combat.TotemPopEvent;
import ravex.gui.clickgui.ClickGUI;
import ravex.manager.*;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.nativelayer.NativeLayer;
import ravex.nativelayer.NativeLayerImpl;
import ravex.utility.misc.GuiOptimizer;
import ravex.utility.misc.GithubUtility;
import ravex.utility.render.TextureLoader;
import ravex.utility.sound.SoundEventDispatcher;
=======
import ravex.manager.ModuleManager;
import ravex.utility.misc.GithubUtility;
import ravex.utility.nativelib.NativeLibrary;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.client.Minecraft;

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
<<<<<<< HEAD
=======
        
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        try {
            var container = FabricLoader.getInstance().getModContainer("ravex");
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

<<<<<<< HEAD
=======
        
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        try {
            java.io.File file = new java.io.File(
                ravex.loader.RaveXLoader.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()
            );
            if (file.exists() && file.isFile() && file.getName().endsWith(".jar")) {
                return file.getAbsolutePath();
            }
        } catch (Exception ignored) {}

<<<<<<< HEAD
=======
        
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        try {
            java.io.File modsDir = new java.io.File(
                FabricLoader.getInstance().getGameDir().toFile(), "mods"
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
    private static boolean texturesPreloaded = false;

    @Override
    public void onInitializeClient() {
<<<<<<< HEAD
        try {
            org.apache.logging.log4j.core.config.Configurator.setLevel("net.minecraft.class_1059", org.apache.logging.log4j.Level.WARN);
            org.apache.logging.log4j.core.config.Configurator.setLevel("net.minecraft.class_310", org.apache.logging.log4j.Level.WARN);
            org.apache.logging.log4j.core.config.Configurator.setLevel("net.minecraft.class_1140", org.apache.logging.log4j.Level.WARN);
            org.apache.logging.log4j.core.config.Configurator.setLevel("net.minecraft.class_3304", org.apache.logging.log4j.Level.WARN);
            org.apache.logging.log4j.core.config.Configurator.setLevel("net.minecraft.class_391", org.apache.logging.log4j.Level.WARN);
        } catch (Throwable ignored) {}
        String osName = ravex.loader.RaveXLoader.getDetailedOSName();
        LOGGER.info("RaveX v{} on {}", version, osName);

        registerServices();

        NativeLayer nativeLayer = ServiceLocator.resolve(NativeLayer.class);
        nativeLayer.load();
        nativeLayer.checkNatives();
        GuiOptimizer.optimize();

        ModuleManager moduleManager = ServiceLocator.resolve(ModuleManager.class);
        moduleManager.init();
        LOGGER.info("Registered {} modules", moduleManager.getModules().size());

        try {
            ServiceLocator.resolve(AddonManager.class).init();
        } catch (Exception e) {
            LOGGER.error("AddonManager init failed", e);
        }

        ServiceLocator.resolve(MacroManager.class).load();
        int mc = ServiceLocator.resolve(MacroManager.class).getMacros().size();
        if (mc > 0) LOGGER.info("Loaded {} macro(s)", mc);

        ServiceLocator.resolve(ProfileManager.class).load();
        int pc = ServiceLocator.resolve(ProfileManager.class).getProfiles().size();
        if (pc > 0) LOGGER.info("Loaded {} profile(s)", pc);

        try {
            ServiceLocator.resolve(ConfigManager.class).load("default");
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        } catch (Exception e) {
            LOGGER.error("Failed to load default config", e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
<<<<<<< HEAD
                ServiceLocator.resolveOrNull(AddonManager.class);
                if (ServiceLocator.isRegistered(AddonManager.class)) {
                    ServiceLocator.resolve(AddonManager.class).shutdown();
                }
            } catch (Throwable ignored) {}
            if (ServiceLocator.isRegistered(ConfigManager.class)) {
                ServiceLocator.resolve(ConfigManager.class).save("default");
            }
        }));

        new Thread(GithubUtility::checkUpdates).start();

        var bus = EventBusHolder.get();
        bus.subscribe(new SoundEventDispatcher());
        bus.subscribe(new NotificationHandler());
    }

    private void registerServices() {
        ServiceLocator.register(NativeLayer.class, new NativeLayerImpl());

        ServiceLocator.register(ModuleManager.class, ModuleManager.INSTANCE);
        ServiceLocator.register(ConfigManager.class, ConfigManager.INSTANCE);
        ServiceLocator.register(ProfileManager.class, ProfileManager.INSTANCE);
        ServiceLocator.register(MacroManager.class, MacroManager.INSTANCE);
        ServiceLocator.register(AddonManager.class, AddonManager.INSTANCE);
        ServiceLocator.register(LuaManager.class, LuaManager.INSTANCE);
        ServiceLocator.register(CmdManager.class, CmdManager.INSTANCE);
        ServiceLocator.register(FriendManager.class, FriendManager.INSTANCE);
        ServiceLocator.register(InventoryManager.class, InventoryManager.INSTANCE);
        ServiceLocator.register(RotationManager.class, RotationManager.INSTANCE);
        ServiceLocator.register(LayoutManager.class, LayoutManager.INSTANCE);
        ServiceLocator.register(ShaderManager.class, ShaderManager.INSTANCE);
        ServiceLocator.register(NotificationManager.class, new NotificationManager());
        ServiceLocator.register(CrystalManager.class, CrystalManager.INSTANCE);

        // Register pending objects for @Inject
        Injector.injectAll();

        LOGGER.info("[RaveX] Services registered");
=======
                ravex.manager.AddonManager.INSTANCE.shutdown();
            } catch (Throwable ignored) {}
            ravex.manager.ConfigManager.INSTANCE.save("default");
        }));

        new Thread(ravex.utility.misc.GithubUtility::checkUpdates).start();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    }

    public static void onClientTick() {
        if (!texturesPreloaded) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getWindow() != null) {
                TextureLoader.preloadAll();
                texturesPreloaded = true;
            }
        }

        if (!loaderProcessClosed) {
            closeLoaderProcess();
            createReadySignal();
            loaderProcessClosed = true;
        }

<<<<<<< HEAD
        EventBusHolder.get().post(new TickEvent.Client());
<<<<<<< HEAD
=======
        ModuleManager.INSTANCE.onTick();
        ravex.manager.MacroManager.INSTANCE.onTick();
        ravex.manager.LuaManager.INSTANCE.onTick(); 
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
=======
        ravex.addon.core.CAddonManager.onTick();
        ServiceLocator.resolve(ModuleManager.class).onTick();
>>>>>>> 0ab37177398daa0e9880b2ec0d3ee76a2dbed416

        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() == null) return;

        com.mojang.blaze3d.platform.Window window = mc.getWindow();

<<<<<<< HEAD
=======
        
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
        boolean isDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (isDown && !rightShiftWasDown) {
            if (mc.screen instanceof ClickGUI) {
                mc.setScreen(null);
            } else {
                mc.setScreen(new ClickGUI());
            }
        }
        rightShiftWasDown = isDown;

<<<<<<< HEAD
        for (Module m : ServiceLocator.resolve(ModuleManager.class).getModules()) {
=======
        
        for (ravex.modules.Module m : ModuleManager.INSTANCE.getModules()) {
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
            int bind = m.getKeyBind();
            if (bind > 0 && bind < keysState.length) {
                boolean isKeyBindDown = com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, bind);
                if (isKeyBindDown && !keysState[bind]) {
                    if (mc.screen == null || mc.screen instanceof ClickGUI) {
                        m.toggle();
                    }
                }
                keysState[bind] = isKeyBindDown;
            }
        }
    }

    private static class NotificationHandler {
        @Subscribe
        public void onModuleToggle(ModuleToggleEvent event) {
            ravex.modules.client.Notifications.notifyToggle(event.getModule(), event.isEnabled());
        }
    }
}
