package ravex.utility.player.rotation;

import net.minecraft.client.Minecraft;
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

    public void reset() {
        yaw = 0;
        pitch = 0;
        hasRotation = false;
        initialized = false;
    }

    public boolean isRotationAligned(Minecraft mc, Vec3 target, float tolerance) {
        if (mc.player == null) return false;
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
}
