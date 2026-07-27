package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import ravex.utility.player.rotation.RotationUtility;
import ravex.utility.player.rotation.SilentRotationUtility;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "AntiAim", category = "net.minecraft.world.entity.player.Player")
public class AntiAim {
    @Parameter(name = "YawMode", modes = {"Spin", "Jitter", "Static", "Random"})
    public String yawMode = "Spin";
    @Parameter(name = "PitchMode", modes = {"Down", "Up", "Jitter", "Static", "None"})
    public String pitchMode = "Down";
    @Parameter(name = "YawSpeed", min = 1.0, max = 90.0, step = 1.0)
    public double yawSpeed = 30.0;
    @Parameter(name = "YawOffset", min = -180.0, max = 180.0, step = 1.0)
    public double yawOffset = 0.0;
    @Parameter(name = "PitchOffset", min = -90.0, max = 90.0, step = 1.0)
    public double pitchOffset = 0.0;
    @Parameter(name = "StaticPitch", min = -90.0, max = 90.0, step = 1.0)
    public double staticPitch = 0.0;
    @Parameter(name = "YawJitter", min = 5.0, max = 180.0, step = 5.0)
    public double yawJitterAmount = 90.0;
    @Parameter(name = "PitchJitter", min = 5.0, max = 90.0, step = 5.0)
    public double pitchJitterAmount = 90.0;
    @Parameter(name = "Silent")
    public boolean silent = true;

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
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;
        ticks++;

        spinYaw += (float) yawSpeed;
        spinYaw = RotationUtility.normalizeYaw(spinYaw);

        float targetYaw = mc.player.getYRot();
        float targetPitch = mc.player.getXRot();

        String yawModeStr = yawMode;
        switch (yawModeStr) {
            case "Spin" -> targetYaw = spinYaw;
            case "Jitter" -> {
                float jitter = (float) yawJitterAmount;
                targetYaw = mc.player.getYRot() + (ticks % 2 == 0 ? jitter : -jitter);
            }
            case "Static" -> targetYaw = mc.player.getYRot() + 180f;
            case "Random" -> {
                float range = (float) yawJitterAmount;
                targetYaw = mc.player.getYRot() + ThreadLocalRandom.current().nextFloat(-range, range);
            }
        }

        targetYaw += (float) yawOffset;
        targetYaw = RotationUtility.normalizeYaw(targetYaw);

        String pitchModeStr = pitchMode;
        switch (pitchModeStr) {
            case "Down" -> targetPitch = 90f;
            case "Up" -> targetPitch = -90f;
            case "Jitter" -> {
                float jitter = (float) pitchJitterAmount;
                targetPitch = ticks % 2 == 0 ? jitter : -jitter;
            }
            case "Static" -> targetPitch = (float) staticPitch;
            case "None" -> {}
        }

        targetPitch += (float) pitchOffset;
        targetPitch = RotationUtility.clampPitch(targetPitch);

        if (silent) {
            silentRotation.set(targetYaw, targetPitch);
        } else {
            mc.player.setYRot(targetYaw);
            mc.player.setXRot(targetPitch);
        }
    }






}