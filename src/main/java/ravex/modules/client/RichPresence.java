package ravex.modules.client;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import ravex.utility.player.PlayerUtility;
import ravex.utility.nativelib.NativeLoader;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "RichPresence", category = "Client")
public class RichPresence {
    private static final String CLIENT_ID = "1517835260799484034";

    @Parameter(name = "LargeImage")
    public String largeImage = "Video";
    @Parameter(name = "ShowHP")
    public boolean showHP = true;
    @Parameter(name = "ShowCoords")
    public boolean showCoords = false;
    @Parameter(name = "ShowIP")
    public boolean showIP = true;
    @Parameter(name = "ShowPing")
    public boolean showPing = true;
    @Parameter(name = "ShowButton")
    public boolean showButton = true;
    @Parameter(name = "ShowOS")
    public boolean showOS = true;

    private static native void nativeInitialize(String clientId);
    private static native void nativeShutdown();
    private static native void nativeUpdatePresence(String state, String details, long startTimestamp,
                                                     boolean showOS, boolean showButton, String largeImageKey);
    private static native boolean nativeIsConnected();

    private Thread updateThread;
    private volatile boolean running = false;
    public void onEnable() {
        ravex.RaveX.LOGGER.info("[RichPresence] onEnable called");
        if (!NativeLoader.loadLibrary("ravex_dc")) {
            ravex.RaveX.LOGGER.warn("[RichPresence] native dc library is not available, module stays off");
            return;
        }
        ravex.RaveX.LOGGER.info("[RichPresence] native library loaded, starting thread");
        running = true;
        updateThread = new Thread(() -> {
            try {
                nativeInitialize(CLIENT_ID);
                ravex.RaveX.LOGGER.info("[RichPresence] nativeInitialize succeeded");
            } catch (Throwable t) {
                System.err.println("[RichPresence] nativeInitialize failed: " + t);
                t.printStackTrace();
                running = false;
                return;
            }
            long startTime = System.currentTimeMillis();
            while (running) {
                try {
                    updatePresence(startTime);
                    ravex.RaveX.LOGGER.info("[RichPresence] isConnected=" + nativeIsConnected());
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
            try {
                nativeShutdown();
            } catch (Throwable ignored) {}
        }, "RaveX-RichPresence");
        updateThread.setDaemon(true);
        updateThread.start();
    }
    public void onDisable() {
        running = false;
        if (updateThread != null) {
            updateThread.interrupt();
            try {
                updateThread.join(2000);
            } catch (InterruptedException ignored) {}
            updateThread = null;
        }
        try {
            nativeShutdown();
        } catch (Throwable ignored) {}
    }
    private void updatePresence(long startTime) {
        var mc = MinecraftWrapper.getWrapper();
        String details;
        String state;
        if (mc.getPlayer() == null || mc.getLevel() == null) {
            details = "Menu";
            state = "InMainMenu";
        } else {
            details = "RaveX - " + mc.getPlayer().getGameProfile().name();
            StringBuilder stateBuilder = new StringBuilder();
            if (showHP) {
                int hp = (int) Math.ceil(PlayerUtility.getHealth(mc.getPlayer()));
                int maxHp = (int) Math.ceil(PlayerUtility.getMaxHealth(mc.getPlayer()));
                stateBuilder.append("HP ").append(hp).append("/").append(maxHp);
            }
            if (showIP) {
                if (stateBuilder.length() > 0) stateBuilder.append(" | ");
                ServerData server = mc.getCurrentServer();
                if (server != null) {
                    stateBuilder.append(server.ip);
                }
            }
            if (showPing) {
                if (stateBuilder.length() > 0) stateBuilder.append(" | ");
                if (mc.getConnection() != null) {
                    PlayerInfo info = mc.getConnection().getPlayerInfo(mc.getPlayer().getUUID());
                    if (info != null) {
                        stateBuilder.append(info.getLatency()).append("ms");
                    }
                }
            }
            if (showCoords) {
                if (stateBuilder.length() > 0) stateBuilder.append(" | ");
                stateBuilder.append(String.format("XYZ: %.0f, %.0f, %.0f", mc.getPlayer().getX(), mc.getPlayer().getY(), mc.getPlayer().getZ()));
            } else {
                int enabledCount = 0;
                for (var m : ravex.manager.ModuleManager.INSTANCE.getModules()) {
                    if (m.getEnabled()) {
                        enabledCount++;
                    }
                }
                if (stateBuilder.length() > 0) stateBuilder.append(" | ");
                stateBuilder.append(enabledCount).append(" modules");
            }
            state = stateBuilder.toString();
        }
        try {
            nativeUpdatePresence(state, details, startTime, showOS, showButton, largeImage);
            ravex.RaveX.LOGGER.info("[RichPresence] sent: state=" + state + ", details=" + details);
        } catch (Throwable t) {
            System.err.println("[RichPresence] nativeUpdatePresence failed: " + t);
            t.printStackTrace();
        }
    }
}
