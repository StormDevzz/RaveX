package ravex.modules.misc;

import net.minecraft.client.Minecraft;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class LagNotify extends Module {
    public static final LagNotify INSTANCE = new LagNotify();

    public final NumberParameter threshold = new NumberParameter("Threshold TPS", 15.0, 5.0, 20.0, 1.0);
    public final BooleanParameter sound = new BooleanParameter("Sound", true);

    private long lastRealTime = 0;
    private long lastGameTick = -1;
    private float smoothedTPS = 20.0f;
    private boolean wasLagging = false;

    private LagNotify() {
        super("LagNotify", Category.MISC);
        addParameter(threshold);
        addParameter(sound);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
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

            double tpsThreshold = threshold.getValue();
            boolean isLagging = smoothedTPS < tpsThreshold;

            if (isLagging && !wasLagging) {
                String tps = String.format("%.1f", smoothedTPS);
                ravex.utility.notification.NotificationManager.add(
                        "Server lag: " + tps + " TPS", 0xFFFFCC33, 3000);
                if (sound.getValue()) {
                    ravex.utility.sound.SoundUtility.playFailure();
                }
            } else if (!isLagging && wasLagging) {
                String tps = String.format("%.1f", smoothedTPS);
                ravex.utility.notification.NotificationManager.add(
                        "Server recovered: " + tps + " TPS", 0xFF44FF88, 2500);
            }

            wasLagging = isLagging;
        }
    }
}
