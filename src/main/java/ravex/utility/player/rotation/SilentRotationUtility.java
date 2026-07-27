package ravex.utility.player.rotation;

import net.minecraft.client.Minecraft;
import ravex.mcwrapper.MinecraftWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SilentRotationUtility {
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

    public void init(MinecraftWrapper mc) {
        var _mc = mc.getRaw();
        if (_mc.player != null) init(_mc.player.getYRot(), _mc.player.getXRot());
    }

    public void set(float targetYaw, float targetPitch) {
        yaw = targetYaw;
        pitch = targetPitch;
        hasRotation = true;
    }

    public void set(float[] angles) {
        if (angles.length >= 2) set(angles[0], angles[1]);
    }

    public void setAnglesTo(MinecraftWrapper mc, Vec3 target) {
        var _mc = mc.getRaw();
        float[] angles = RotationUtility.anglesTo(_mc.player.getEyePosition(), target);
        set(angles);
    }

    public void setAnglesTo(MinecraftWrapper mc, Entity target) {
        var _mc = mc.getRaw();
        float[] angles = RotationUtility.anglesTo(_mc.player, target);
        set(angles);
    }

    public void setAnglesTo(MinecraftWrapper mc, Entity target, double yOffset) {
        var _mc = mc.getRaw();
        float[] angles = RotationUtility.anglesTo(_mc.player, target, yOffset);
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

    public void reset() {
        yaw = 0;
        pitch = 0;
        hasRotation = false;
        initialized = false;
    }

    public boolean isRotationAligned(MinecraftWrapper mc, Vec3 target, float tolerance) {
        var _mc = mc.getRaw();
        if (_mc.player == null) return false;
        float[] targetAngles = RotationUtility.anglesTo(_mc.player.getEyePosition(), target);
        float yawDiff = Math.abs(RotationUtility.normalizeYaw(targetAngles[0] - _mc.player.getYRot()));
        float pitchDiff = Math.abs(targetAngles[1] - _mc.player.getXRot());
        return yawDiff <= tolerance && pitchDiff <= tolerance;
    }
}
