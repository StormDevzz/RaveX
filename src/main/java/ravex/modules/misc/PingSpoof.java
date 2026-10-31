package ravex.modules.misc;
import ravex.event.Subscribe;
import ravex.event.client.TickEvent;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;
import ravex.utility.network.NetworkUtility;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import java.util.ArrayDeque;

@Module(name = "PingSpoof", category = "Misc")
public class PingSpoof {
    @Parameter(name = "Ping", min = 0, max = 30000, step = 100)
    public double ping = 1000;

    // server times the keep alive reply, so delay it
    private static final ArrayDeque<long[]> QUEUE = new ArrayDeque<>();
    private static boolean loggedOnce;

    public int getSpoofedPing() {
        if (!Modules.enabled(PingSpoof.class) || ping <= 0) return -1;
        return (int) ping;
    }

    // true when the reply is queued and the vanilla send is skipped
    public static boolean delayKeepAlive(long id) {
        PingSpoof ps = Modules.get(PingSpoof.class);
        if (ps == null || !Modules.enabled(PingSpoof.class) || ps.ping <= 0) return false;
        // server kicks after 30s, stay under it
        long delay = (long) Math.min(ps.ping, 25000.0);
        if (!loggedOnce) {
            loggedOnce = true;
            System.out.println("[PingSpoof] holding keep alive replies for " + delay + "ms");
        }
        synchronized (QUEUE) {
            QUEUE.add(new long[]{id, System.currentTimeMillis() + delay});
        }
        return true;
    }

    @Subscribe
    public void onTick(TickEvent.Client event) {
        if (!Modules.enabled(PingSpoof.class)) {
            synchronized (QUEUE) {
                QUEUE.clear();
            }
            return;
        }
        long now = System.currentTimeMillis();
        while (true) {
            long[] due;
            synchronized (QUEUE) {
                due = QUEUE.peek();
            }
            if (due == null || due[1] > now) return;
            synchronized (QUEUE) {
                QUEUE.poll();
            }
            NetworkUtility.sendPacket(new ServerboundKeepAlivePacket(due[0]));
        }
    }
}
