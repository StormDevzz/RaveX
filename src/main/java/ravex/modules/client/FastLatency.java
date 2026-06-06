package ravex.modules.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;

/**
 * FastLatency - sends ping packets more frequently to get accurate real-time latency.
 * Uses ClientboundPingPacket/ServerboundPongPacket (common protocol) which is available in-game.
 */
public class FastLatency extends Module {
    public static final FastLatency INSTANCE = new FastLatency();

    public final NumberParameter interval = new NumberParameter("Interval (ms)", 1000.0, 200.0, 5000.0, 100.0);

    private long lastPingTime = 0;
    private long lastPingSentAt = 0;
    private int measuredPing = -1;

    private FastLatency() {
        super("FastLatency", Category.CLIENT);
        addParameter(interval);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getConnection() == null) return;
        if (mc.level == null) return;

        long now = System.currentTimeMillis();
        long intervalMs = interval.getValue().longValue();

        if (now - lastPingTime >= intervalMs) {
            lastPingTime = now;
            lastPingSentAt = now;
            // Send common ping packet — works in-game (server responds with Pong)
            try {
                mc.getConnection().send(new net.minecraft.network.protocol.common.ServerboundPongPacket((int)(lastPingSentAt & 0x7FFFFFFF)));
            } catch (Exception ignored) {}
        }
    }

    /**
     * Called by network mixin when we receive a ClientboundPingPacket or Pong response.
     * We measure RTT using our own timestamp.
     */
    public void handlePong(int id) {
        int sentId = (int)(lastPingSentAt & 0x7FFFFFFF);
        if (id == sentId) {
            measuredPing = (int)(System.currentTimeMillis() - lastPingSentAt);
        }
    }

    public int getMeasuredPing() {
        return measuredPing;
    }

    /**
     * Returns the best available ping value (our measured, or fallback to vanilla).
     */
    public static int getDisplayPing() {
        Minecraft mc = Minecraft.getInstance();
        if (INSTANCE.getEnabled() && INSTANCE.measuredPing >= 0) {
            return INSTANCE.measuredPing;
        }
        if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return info != null ? info.getLatency() : -1;
        }
        return -1;
    }
}
