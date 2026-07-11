package ravex.modules.client;
import ravex.manager.ModuleManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

public class FastLatency extends Module {
    public final NumberParameter interval = new NumberParameter("Interval", 1000.0, 200.0, 5000.0, 100.0);
    private long lastPingTime = 0;
    private long lastPingSentAt = 0;
    private int measuredPing = -1;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null)
            return;
        if (mc.level == null)
            return;
        long now = System.currentTimeMillis();
        long intervalMs = interval.getValue().longValue();
        if (now - lastPingTime >= intervalMs) {
            lastPingTime = now;
            lastPingSentAt = now;
            try {
                mc.getConnection().send(new net.minecraft.network.protocol.common.ServerboundPongPacket(
                        (int) (lastPingSentAt & 0x7FFFFFFF)));
            } catch (Exception ignored) {
            }
        }
    }

    public void handlePong(int id) {
        int sentId = (int) (lastPingSentAt & 0x7FFFFFFF);
        if (id == sentId) {
            measuredPing = (int) (System.currentTimeMillis() - lastPingSentAt);
        }
    }

    public int getMeasuredPing() {
        return measuredPing;
    }

    public static int getDisplayPing() {
        Minecraft mc = Minecraft.getInstance();
        if (ModuleManager.get(FastLatency.class).getEnabled() && ModuleManager.get(FastLatency.class).measuredPing >= 0) {
            return ModuleManager.get(FastLatency.class).measuredPing;
        }
        if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return info != null ? info.getLatency() : -1;
        }
        return -1;
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(FastLatency.class);
    }

    public static FastLatency itz() {
        return ModuleManager.get(FastLatency.class);
    }
}
