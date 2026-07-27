package ravex.modules.client;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.StringParameter;
import ravex.manager.LuaManager;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "RichPresence", category = "Client")
public class RichPresence {
    @Parameter(name = "LargeImage")
    public String largeImage = "ravexdc";
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
    private Thread updateThread;
    private volatile boolean running = false;
    public void onEnable() {
        running = true;
        updateThread = new Thread(() -> {
            try {
                LuaManager.INSTANCE.discordConnect();
            } catch (Throwable t) {
                System.err.println("[RichPresence] discordConnect failed: " + t.getMessage());
                running = false;
                return;
            }
            long startTime = System.currentTimeMillis();
            while (running) {
                try {
                    updatePresence(startTime);
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    break;
                } catch (Throwable ignored) {}
            }
            try {
                LuaManager.INSTANCE.discordClearActivity();
                LuaManager.INSTANCE.discordDisconnect();
            } catch (Throwable ignored) {}
        }, "RaveX-RichPresence");
        updateThread.setDaemon(true);
        updateThread.start();
    }
    public void onDisable() {
        running = false;
        if (updateThread != null) {
            updateThread.interrupt();
            updateThread = null;
        }
        try {
            LuaManager.INSTANCE.discordClearActivity();
            LuaManager.INSTANCE.discordDisconnect();
        } catch (Throwable ignored) {}
    }
    private void updatePresence(long startTime) {
        var mc = MinecraftWrapper.getInstance();
        String details;
        String state;
        if (mc.player == null || mc.level == null) {
            details = "Menu";
            state = "InMainMenu";
        } else {
            details = "RaveX — " + mc.player.getGameProfile().name();
            StringBuilder stateBuilder = new StringBuilder();
            if (showHP) {
                int hp = (int) Math.ceil(mc.player.getHealth());
                int maxHp = (int) Math.ceil(mc.player.getMaxHealth());
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
                    PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                    if (info != null) {
                        stateBuilder.append(info.getLatency()).append("ms");
                    }
                }
            }
            if (showCoords) {
                if (stateBuilder.length() > 0) stateBuilder.append(" | ");
                stateBuilder.append(String.format("XYZ: %.0f, %.0f, %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
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
            LuaManager.INSTANCE.discordSetActivity(details, state, startTime, showOS, showButton);
        } catch (Throwable t) {
            System.err.println("[RichPresence] discordSetActivity failed: " + t.getMessage());
        }
    }






}