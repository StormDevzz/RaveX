package ravex.modules.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.DiscordBuild;
import com.jagrosh.discordipc.entities.ActivityType;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import ravex.manager.ModuleManager;
import ravex.manager.NotificationManager;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.player.PlayerUtility;

@Module(name = "RichPresence", category = "Client")
public class RichPresence {
    private static final long CLIENT_ID = 1517835260799484034L;
    private static final long UPDATE_INTERVAL_MS = 5000L;

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
    @Parameter(name = "RunInInvisible")
    public boolean runInInvisible = true;

    private volatile boolean running = false;
    private volatile IPCClient client;
    private Thread updateThread;
    private long startTime;
    private boolean firstSendLogged = false;
    private volatile boolean ready = false;

    public void onEnable() {
        if (running) return;
        running = true;
        startTime = System.currentTimeMillis();
        updateThread = new Thread(this::runLoop, "RaveX-RichPresence");
        updateThread.setDaemon(true);
        updateThread.start();
    }

    public void onDisable() {
        running = false;
        if (updateThread != null) {
            updateThread.interrupt();
            try {
                updateThread.join(1000L);
            } catch (InterruptedException ignored) {}
            updateThread = null;
        }
        closeClient();
    }

    private void runLoop() {
        long connectDeadline = System.currentTimeMillis() + 4000L;
        while (running && client == null) {
            try {
                IPCClient newClient = new IPCClient(CLIENT_ID);
                newClient.setListener(new RpcListener());
                ready = false;
                newClient.connect(DiscordBuild.ANY);
                long readyDeadline = System.currentTimeMillis() + 2500L;
                while (running && !ready && System.currentTimeMillis() < readyDeadline) {
                    sleep(25L);
                }
                if (newClient.getStatus() != PipeStatus.CONNECTED) {
                    newClient.close();
                    if (System.currentTimeMillis() >= connectDeadline) {
                        fail("Discord не найден! Запустите Discord.");
                        return;
                    }
                    sleep(400L);
                    continue;
                }
                client = newClient;
                ravex.RaveX.LOGGER.info("[RichPresence] connected to Discord");
            } catch (NoDiscordClientException e) {
                if (System.currentTimeMillis() >= connectDeadline) {
                    fail("Discord не найден! Запустите Discord.");
                    return;
                }
                sleep(400L);
            } catch (Throwable t) {
                if (System.currentTimeMillis() >= connectDeadline) {
                    fail("Не удалось подключиться к Discord!");
                    return;
                }
                sleep(400L);
            }
        }
        if (client == null) return;

        sendPresence();
        while (running) {
            sleep(UPDATE_INTERVAL_MS);
            if (!running) break;
            IPCClient current = client;
            if (current == null || current.getStatus() != PipeStatus.CONNECTED) {
                fail("Процесс Discord закрыт!");
                return;
            }
            if (!runInInvisible && !Minecraft.getInstance().isWindowActive()) continue;
            sendPresence();
        }
    }

    private void sendPresence() {
        IPCClient current = client;
        if (current == null) return;
        try {
            current.sendRichPresence(buildPresence());
            if (!firstSendLogged) {
                firstSendLogged = true;
                ravex.RaveX.LOGGER.info("[RichPresence] presence sent");
            }
        } catch (Throwable t) {
            ravex.RaveX.LOGGER.warn("[RichPresence] send failed", t);
            fail("Ошибка отправки статуса Discord!");
        }
    }

    private com.jagrosh.discordipc.entities.RichPresence buildPresence() {
        MinecraftWrapper mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        String details;
        StringBuilder state = new StringBuilder();

        if (player == null || mc.getLevel() == null) {
            details = "RaveX - Main Menu";
            state.append("In Main Menu");
        } else {
            details = "RaveX - " + player.getGameProfile().name();
            if (showHP) {
                int hp = (int) Math.ceil(PlayerUtility.getHealth(player));
                int maxHp = (int) Math.ceil(PlayerUtility.getMaxHealth(player));
                state.append("HP ").append(hp).append('/').append(maxHp);
            }
            if (showIP) {
                if (state.length() > 0) state.append(" | ");
                ServerData server = mc.getCurrentServer();
                state.append(server != null && server.ip != null && !server.ip.isBlank() ? server.ip : "Singleplayer");
            }
            if (showPing && mc.getConnection() != null) {
                PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
                if (info != null) {
                    if (state.length() > 0) state.append(" | ");
                    state.append(info.getLatency()).append("ms");
                }
            }
            if (showCoords) {
                if (state.length() > 0) state.append(" | ");
                state.append(String.format("XYZ: %.0f, %.0f, %.0f", player.getX(), player.getY(), player.getZ()));
            } else {
                int enabledCount = 0;
                for (var module : ModuleManager.INSTANCE.getModules()) {
                    if (module.getEnabled()) enabledCount++;
                }
                if (state.length() > 0) state.append(" | ");
                state.append(enabledCount).append(" modules");
            }
        }

        if (showOS) {
            if (state.length() > 0) state.append(" | ");
            state.append("OS: ").append(System.getProperty("os.name", "Unknown"));
        }

        com.jagrosh.discordipc.entities.RichPresence.Builder builder = new com.jagrosh.discordipc.entities.RichPresence.Builder()
                .setActivityType(ActivityType.Playing)
                .setDetails(clamp(details))
                .setStartTimestamp(startTime / 1000L)
                .setLargeImage(largeImage, "RaveX");

        String stateText = clamp(state.toString());
        if (!stateText.isEmpty()) builder.setState(stateText);

        if (showButton) {
            JsonArray buttons = new JsonArray();
            JsonObject button = new JsonObject();
            button.addProperty("label", "Download RaveX");
            button.addProperty("url", "https://github.com/StormDevzz/RaveX");
            buttons.add(button);
            builder.setButtons(buttons);
        }

        return builder.build();
    }

    private String clamp(String text) {
        return text.length() <= 128 ? text : text.substring(0, 128);
    }

    private void fail(String message) {
        if (!running) return;
        running = false;
        ravex.RaveX.LOGGER.warn("[RichPresence] " + message);
        Minecraft.getInstance().execute(() -> {
            NotificationManager.addToast(message, 0xFFFF5555, NotificationManager.ToastType.DISABLE);
            var module = ModuleManager.delegate(RichPresence.class);
            if (module != null && module.getEnabled()) module.setEnabled(false);
        });
    }

    private void closeClient() {
        IPCClient current = client;
        client = null;
        if (current != null) {
            try {
                current.close();
            } catch (Throwable ignored) {}
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    private final class RpcListener implements IPCListener {
        @Override
        public void onPacketSent(IPCClient client, Packet packet) {}

        @Override
        public void onPacketReceived(IPCClient client, Packet packet) {}

        @Override
        public void onActivityJoin(IPCClient client, String secret) {}

        @Override
        public void onActivitySpectate(IPCClient client, String secret) {}

        @Override
        public void onActivityJoinRequest(IPCClient client, String secret, User user) {}

        @Override
        public void onReady(IPCClient client) {
            ready = true;
        }

        @Override
        public void onClose(IPCClient client, JsonObject json) {
            fail("Процесс Discord закрыт!");
        }

        @Override
        public void onDisconnect(IPCClient client, Throwable t) {
            fail("Discord отключился!");
        }
    }
}
