package ravex.utility.player.rotation;

import net.minecraft.client.Minecraft;
<<<<<<< HEAD
import net.minecraft.world.entity.Entity;
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import net.minecraft.world.phys.Vec3;

public class SilentRotation {
    public float yaw;
    public float pitch;
    public boolean hasRotation;
    public float lastYaw;
    public float lastPitch;
    public boolean initialized;

    public void init(float currentYaw, float currentPitch) {
        lastYaw = currentYaw;
        lastPitch = currentPitch;
        initialized = true;
    }

    public void init(Minecraft mc) {
        if (mc.player != null) init(mc.player.getYRot(), mc.player.getXRot());
    }

    public void set(float targetYaw, float targetPitch) {
        yaw = targetYaw;
        pitch = targetPitch;
        hasRotation = true;
    }

    public void set(float[] angles) {
        if (angles.length >= 2) set(angles[0], angles[1]);
    }

<<<<<<< HEAD
    public void setAnglesTo(Minecraft mc, Vec3 target) {
        float[] angles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
        set(angles);
    }

    public void setAnglesTo(Minecraft mc, Entity target) {
        float[] angles = RotationUtility.anglesTo(mc.player, target);
        set(angles);
    }

    public void setAnglesTo(Minecraft mc, Entity target, double yOffset) {
        float[] angles = RotationUtility.anglesTo(mc.player, target, yOffset);
        set(angles);
    }

    public void setLimited(float targetYaw, float targetPitch, float maxSpeed) {
        float[] limited = AimUtility.limitAngles(
            initialized ? lastYaw : yaw, targetYaw,
            initialized ? lastPitch : pitch, targetPitch,
            maxSpeed
        );
        set(limited[0], limited[1]);
    }

    public void setRandomized(float targetYaw, float targetPitch, double randomAmount) {
        float[] randomized = AimUtility.randomize(targetYaw, targetPitch, randomAmount);
        set(randomized[0], randomized[1]);
    }

=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public void reset() {
        yaw = 0;
        pitch = 0;
        hasRotation = false;
        initialized = false;
    }

    public boolean isRotationAligned(Minecraft mc, Vec3 target, float tolerance) {
        if (mc.player == null) return false;
<<<<<<< HEAD
        float[] targetAngles = RotationUtility.anglesTo(mc.player.getEyePosition(), target);
        float yawDiff = Math.abs(RotationUtility.normalizeYaw(targetAngles[0] - mc.player.getYRot()));
        float pitchDiff = Math.abs(targetAngles[1] - mc.player.getXRot());
        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }
=======
        Vec3 eyes = mc.player.getEyePosition();
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        float yawDiff = Math.abs(normalizeAngle(targetYaw - mc.player.getYRot()));
        float pitchDiff = Math.abs(targetPitch - mc.player.getXRot());
        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }

    public static float normalizeAngle(float angle) {
        angle %= 360;
        if (angle > 180) angle -= 360;
        if (angle < -180) angle += 360;
        return angle;
    }
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
