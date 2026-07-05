package ravex.modules.client;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.StringParameter;
import ravex.manager.LuaManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
public class RichPresence extends Module {
    public static final RichPresence INSTANCE = new RichPresence();
    public final StringParameter largeImage = new StringParameter("Large Image", "ravexdc");
    public final BooleanParameter showHP     = new BooleanParameter("Show HP",     true);
    public final BooleanParameter showCoords = new BooleanParameter("Show Coords", false);
    public final BooleanParameter showIP     = new BooleanParameter("Show IP",     true);
    public final BooleanParameter showPing   = new BooleanParameter("Show Ping",   true);
    public final BooleanParameter showButton = new BooleanParameter("Show Button", true);
    public final BooleanParameter showOS     = new BooleanParameter("Show OS",     true);
    private Thread updateThread;
    private volatile boolean running = false;

    @Override
    protected void onEnable() {
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
    @Override
    protected void onDisable() {
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
        Minecraft mc = Minecraft.getInstance();
        String details;
        String state;
        if (mc.player == null || mc.level == null) {
            details = "Menu";
            state = "In main menu";
        } else {
            details = "RaveX — " + mc.player.getGameProfile().name();
            StringBuilder stateBuilder = new StringBuilder();
            if (showHP.getValue()) {
                int hp = (int) Math.ceil(mc.player.getHealth());
                int maxHp = (int) Math.ceil(mc.player.getMaxHealth());
                stateBuilder.append("HP ").append(hp).append("/").append(maxHp);
            }
            if (showIP.getValue()) {
                if (stateBuilder.length() > 0) stateBuilder.append(" | ");
                ServerData server = mc.getCurrentServer();
                if (server != null) {
                    stateBuilder.append(server.ip);
                }
            }
            if (showPing.getValue()) {
                if (stateBuilder.length() > 0) stateBuilder.append(" | ");
                if (mc.getConnection() != null) {
                    PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                    if (info != null) {
                        stateBuilder.append(info.getLatency()).append("ms");
                    }
                }
            }
            if (showCoords.getValue()) {
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
            LuaManager.INSTANCE.discordSetActivity(details, state, startTime, showOS.getValue(), showButton.getValue());
        } catch (Throwable t) {
            System.err.println("[RichPresence] discordSetActivity failed: " + t.getMessage());
        }
    }
}
