package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;
import ravex.mcwrapper.MinecraftWrapper;

@ModuleInfo(name = "LagNotify", category = "Misc")
public class LagNotify implements ModuleAccess {
    @Parameter(name = "ThresholdTPS", min = 5.0, max = 20.0, step = 1.0)
    public double threshold = 15.0;
    @Parameter(name = "Sound")
    public boolean sound = true;
    private long lastRealTime = 0;
    private long lastGameTick = -1;
    private float smoothedTPS = 20.0f;
    private boolean wasLagging = false;
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        long gameTick = mc.level.getGameTime();
        if (lastGameTick < 0) {
            lastGameTick = gameTick;
            lastRealTime = now;
            return;
        }
        long elapsed = now - lastRealTime;
        if (elapsed >= 1000) {
            long ticks = gameTick - lastGameTick;
            float measured = (float)(ticks * 1000.0 / elapsed);
            smoothedTPS = smoothedTPS * 0.7f + Math.min(20f, Math.max(0f, measured)) * 0.3f;
            lastGameTick = gameTick;
            lastRealTime = now;
            double tpsThreshold = threshold;
            boolean isLagging = smoothedTPS < tpsThreshold;
            if (isLagging && !wasLagging) {
                String tps = String.format("%.1f", smoothedTPS);
                ravex.manager.NotificationManager.add(
                        "Server lag: " + tps + " TPS", 0xFFFFCC33, 3000);
                if (sound) {
                    EventBusHolder.get().post(new SoundEvent(SoundEvent.Type.FAILURE));
                }
            } else if (!isLagging && wasLagging) {
                String tps = String.format("%.1f", smoothedTPS);
                ravex.manager.NotificationManager.add(
                        "Server recovered: " + tps + " TPS", 0xFF44FF88, 2500);
            }
            wasLagging = isLagging;
        }
    }

    public static LagNotify itz() {
        return ravex.manager.ModuleManager.delegate(LagNotify.class);
    }


}