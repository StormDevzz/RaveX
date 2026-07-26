package ravex.modules.client;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.StringParameter;
import ravex.manager.LuaManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
@ModuleInfo(name = "RichPresence", category = "Client")
public class RichPresence extends ravex.modules.Module {
public final StringParameter largeImage = new StringParameter("LargeImage", "ravexdc");
    public final BooleanParameter showHP     = new BooleanParameter("ShowHP",     true);
    public final BooleanParameter showCoords = new BooleanParameter("ShowCoords", false);
    public final BooleanParameter showIP     = new BooleanParameter("ShowIP",     true);
    public final BooleanParameter showPing   = new BooleanParameter("ShowPing",   true);
    public final BooleanParameter showButton = new BooleanParameter("ShowButton", true);
    public final BooleanParameter showOS     = new BooleanParameter("ShowOS",     true);
    private Thread updateThread;
    private volatile boolean running = false;
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
            state = "InMainMenu";
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

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("RichPresence").getEnabled();
    }

    public static RichPresence itz() {
        return ravex.manager.ModuleManager.delegate(RichPresence.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}