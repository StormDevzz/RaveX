package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "FastLatency", category = "Client")
public class FastLatency {
    @Parameter(name = "Interval", min = 200.0, max = 5000.0, step = 100.0)
    public double interval = 1000.0;
    private long lastPingTime = 0;
    private long lastPingSentAt = 0;
    private int measuredPing = -1;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.getConnection() == null)
            return;
        if (mc.level == null)
            return;
        long now = System.currentTimeMillis();
        long intervalMs = (long) interval;
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
        var mc = MinecraftWrapper.getInstance();
        var fl = Modules.get(FastLatency.class);
        if (Modules.enabled(FastLatency.class) && fl.measuredPing >= 0) {
            return fl.measuredPing;
        }
        if (mc.getConnection() != null && mc.player != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return info != null ? info.getLatency() : -1;
        }
        return -1;
    }






}