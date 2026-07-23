package ravex.modules.player;

import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotation;

public class AntiAim extends Module {
    public final ModeParameter yawMode = new ModeParameter("YawMode", "Spin",
            List.of("Spin", "Jitter", "Static", "Random"));
    public final ModeParameter pitchMode = new ModeParameter("PitchMode", "Down",
            List.of("Down", "Up", "Jitter", "Static", "None"));
    public final NumberParameter yawSpeed = new NumberParameter("YawSpeed", 30.0, 1.0, 90.0, 1.0);
    public final NumberParameter yawOffset = new NumberParameter("YawOffset", 0.0, -180.0, 180.0, 1.0);
    public final NumberParameter pitchOffset = new NumberParameter("PitchOffset", 0.0, -90.0, 90.0, 1.0);
    public final NumberParameter staticPitch = new NumberParameter("StaticPitch", 0.0, -90.0, 90.0, 1.0);
    public final NumberParameter yawJitterAmount = new NumberParameter("YawJitter", 90.0, 5.0, 180.0, 5.0);
    public final NumberParameter pitchJitterAmount = new NumberParameter("PitchJitter", 90.0, 5.0, 90.0, 5.0);
    public final BooleanParameter silent = new BooleanParameter("Silent", true);

    public static final SilentRotation silentRotation = new SilentRotation();
    private float spinYaw = 0;
    private long ticks = 0;

    public static float getSilentYaw() {
        return silentRotation.yaw;
    }

    public static float getSilentPitch() {
        return silentRotation.pitch;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ticks++;

        spinYaw += yawSpeed.getValue().floatValue();
        spinYaw = RotationUtility.normalizeYaw(spinYaw);

        float targetYaw = mc.player.getYRot();
        float targetPitch = mc.player.getXRot();

        String yawModeStr = yawMode.getValue();
        switch (yawModeStr) {
            case "Spin" -> targetYaw = spinYaw;
            case "Jitter" -> {
                float jitter = yawJitterAmount.getValue().floatValue();
                targetYaw = mc.player.getYRot() + (ticks % 2 == 0 ? jitter : -jitter);
            }
            case "Static" -> targetYaw = mc.player.getYRot() + 180f;
            case "Random" -> {
                float range = yawJitterAmount.getValue().floatValue();
                targetYaw = mc.player.getYRot() + ThreadLocalRandom.current().nextFloat(-range, range);
            }
        }

        targetYaw += yawOffset.getValue().floatValue();
        targetYaw = RotationUtility.normalizeYaw(targetYaw);

        String pitchModeStr = pitchMode.getValue();
        switch (pitchModeStr) {
            case "Down" -> targetPitch = 90f;
            case "Up" -> targetPitch = -90f;
            case "Jitter" -> {
                float jitter = pitchJitterAmount.getValue().floatValue();
                targetPitch = ticks % 2 == 0 ? jitter : -jitter;
            }
            case "Static" -> targetPitch = staticPitch.getValue().floatValue();
            case "None" -> {}
        }

        targetPitch += pitchOffset.getValue().floatValue();
        targetPitch = RotationUtility.clampPitch(targetPitch);

        if (silent.getValue()) {
            silentRotation.set(targetYaw, targetPitch);
        } else {
            mc.player.setYRot(targetYaw);
            mc.player.setXRot(targetPitch);
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(AntiAim.class);
    }

    public static AntiAim itz() {
        return ModuleManager.get(AntiAim.class);
    }
}
