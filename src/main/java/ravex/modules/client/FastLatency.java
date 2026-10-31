package ravex.modules.client;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "FastLatency", category = "Client")
public class FastLatency {
    @Parameter(name = "Interval", min = 200.0, max = 5000.0, step = 100.0)
    public double interval = 1000.0;
    // measured ping on every row, not just your own
    @Parameter(name = "ApplyToAll")
    public boolean applyToAll = false;

    public int measuredPing = -1;
    public long lastPongMs = 0L;
    private long lastRequestMs = 0L;

    // server echoes our timestamp, the delta is real rtt
    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(FastLatency.class) || !event.isReceive()) return;
        if (!(event.getPacket() instanceof ClientboundPongResponsePacket pong)) return;
        long rtt = System.currentTimeMillis() - pong.time();
        if (rtt < 0) return;
        measuredPing = (int) Math.min(rtt, 9999);
        lastPongMs = System.currentTimeMillis();
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getConnection() == null || mc.getLevel() == null) return;
        long now = System.currentTimeMillis();
        if (now - lastRequestMs < (long) interval) return;
        lastRequestMs = now;
        NetworkUtility.sendPacket(new ServerboundPingRequestPacket(now));
    }

    // the fresh measured ping, or -1 when the real value should be kept.
    public static int overrideLatency(PlayerInfo info) {
        if (!Modules.enabled(FastLatency.class)) return -1;
        FastLatency fl = Modules.get(FastLatency.class);
        if (fl == null || fl.measuredPing < 0) return -1;
        if (info == null) return -1;
        if (System.currentTimeMillis() - fl.lastPongMs > Math.max(3000L, (long) fl.interval * 3)) return -1;
        if (fl.applyToAll) return fl.measuredPing;
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() != null && info.getProfile().id().equals(mc.getPlayer().getUUID())) {
            return fl.measuredPing;
        }
        return -1;
    }

    public int getMeasuredPing() {
        return measuredPing;
    }
}
