package ravex.utility.player.rotation;

import net.minecraft.world.phys.Vec3;

public class AimUtility {
    public static float limitSpeed(float current, float target, float maxSpeed) {
        if (maxSpeed >= 180) return target;
        float diff = RotationUtility.diffYaw(current, target);
        if (Math.abs(diff) > maxSpeed) diff = Math.signum(diff) * maxSpeed;
        return current + diff;
    }

    public static float[] limitAngles(float currentYaw, float targetYaw, float currentPitch, float targetPitch, float maxSpeed) {
        if (maxSpeed >= 180) return new float[]{targetYaw, targetPitch};
        float yawDiff = RotationUtility.diffYaw(currentYaw, targetYaw);
        float pitchDiff = RotationUtility.diffPitch(currentPitch, targetPitch);
        if (Math.abs(yawDiff) > maxSpeed) yawDiff = Math.signum(yawDiff) * maxSpeed;
        if (Math.abs(pitchDiff) > maxSpeed) pitchDiff = Math.signum(pitchDiff) * maxSpeed;
        return new float[]{currentYaw + yawDiff, currentPitch + pitchDiff};
    }

    public static float randomize(float value, double amount) {
        if (amount <= 0.01) return value;
        return value + (float) ((Math.random() - 0.5) * amount);
    }

    public static float[] randomize(float yaw, float pitch, double amount) {
        if (amount <= 0.01) return new float[]{yaw, pitch};
        return new float[]{
            yaw + (float) ((Math.random() - 0.5) * amount),
            pitch + (float) ((Math.random() - 0.5) * amount)
        };
    }

    public static float[] smoothToTarget(float currentYaw, float targetYaw, float currentPitch, float targetPitch, float speed) {
        return limitAngles(currentYaw, targetYaw, currentPitch, targetPitch, speed / 20f);
    }
}
