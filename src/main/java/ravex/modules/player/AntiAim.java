package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;

@ModuleInfo(name = "AntiAim", category = "net.minecraft.world.entity.player.Player")
public class AntiAim extends ravex.modules.Module {
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

    public static final SilentRotationUtility silentRotation = new SilentRotationUtility();
    private float spinYaw = 0;
    private long ticks = 0;

    public static float getSilentYaw() {
        return silentRotation.yaw;
    }

    public static float getSilentPitch() {
        return silentRotation.pitch;
    }
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
        return ravex.manager.ModuleManager.INSTANCE.getByName("AntiAim").getEnabled();
    }

    public static AntiAim itz() {
        return ravex.manager.ModuleManager.delegate(AntiAim.class);
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