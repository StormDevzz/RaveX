package ravex.modules.misc;
import ravex.event.Subscribe;
import ravex.event.network.PacketEvent;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;
import ravex.utility.nativelib.NativeLoader;

// limits the outgoing packet rate so the server does not kick for flooding.
@Module(name = "NoPacketKick", category = "Misc")
public class NoPacketKick {
    @Parameter(name = "PacketsPerSecond", min = 10, max = 500, step = 10)
    public int packetsPerSecond = 100;
    @Parameter(name = "Burst", min = 1, max = 100, step = 1)
    public int burst = 20;

    private static native void nativeInit(int packetsPerSec, int burstSize);
    private static native boolean nativeShouldAllow();
    private static native int nativeGetRate();
    private static native void nativeReset();

    private boolean limiterReady = false;

    public void onEnable() {
        if (!NativeLoader.loadLibrary("ravex_nopacktkick")) {
            ravex.RaveX.LOGGER.warn("[NoPacketKick] native library is not available, module stays off");
            return;
        }
        nativeInit(packetsPerSecond, burst);
        limiterReady = true;
    }

    public void onDisable() {
        if (!limiterReady) return;
        nativeReset();
        limiterReady = false;
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (!Modules.enabled(NoPacketKick.class) || !event.isSend()) return;
        if (!limiterReady) return;
        if (!nativeShouldAllow()) {
            event.setCancelled(true);
        }
    }

    public int getCurrentRate() {
        if (!limiterReady) return 0;
        return nativeGetRate();
    }
}
