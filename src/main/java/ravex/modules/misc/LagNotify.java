package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.event.EventBusHolder;
import ravex.event.client.SoundEvent;

import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "LagNotify", category = "Misc")
public class LagNotify extends ravex.modules.Module {
public final NumberParameter threshold = new NumberParameter("ThresholdTPS", 15.0, 5.0, 20.0, 1.0);
    public final BooleanParameter sound = new BooleanParameter("Sound", true);
    private long lastRealTime = 0;
    private long lastGameTick = -1;
    private float smoothedTPS = 20.0f;
    private boolean wasLagging = false;
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
                ravex.manager.NotificationManager.add(
                        "Server lag: " + tps + " TPS", 0xFFFFCC33, 3000);
                if (sound.getValue()) {
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