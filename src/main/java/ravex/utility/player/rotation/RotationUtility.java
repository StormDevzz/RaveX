package ravex.utility.player.rotation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class RotationUtility {
    public static float yawTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        return (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
    }

    public static float yawTo(Entity from, Vec3 to) {
        return yawTo(from.getEyePosition(), to);
    }

    public static float yawTo(Entity from, Entity to) {
        return yawTo(from.getEyePosition(), to.getEyePosition());
    }

    public static float pitchTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        return (float) -Math.toDegrees(Math.atan2(dy, dist));
    }

    public static float pitchTo(Entity from, Vec3 to) {
        return pitchTo(from.getEyePosition(), to);
    }

    public static float pitchTo(Entity from, Entity to) {
        return pitchTo(from.getEyePosition(), to.getEyePosition());
    }

    public static float pitchTo(Entity from, Entity to, double yOffset) {
        Vec3 fromPos = from.getEyePosition();
        Vec3 toPos = to.position().add(0, yOffset, 0);
        return pitchTo(fromPos, toPos);
    }

    public static float[] anglesTo(Vec3 from, Vec3 to) {
        return new float[]{yawTo(from, to), pitchTo(from, to)};
    }

    public static float[] anglesTo(Entity from, Vec3 to) {
        return new float[]{yawTo(from, to), pitchTo(from, to)};
    }

    public static float[] anglesTo(Entity from, Entity to) {
        return new float[]{yawTo(from, to), pitchTo(from, to)};
    }

    public static float[] anglesTo(Entity from, Entity to, double yOffset) {
        return new float[]{yawTo(from, to), pitchTo(from, to, yOffset)};
    }

    public static Vec3 getLookVector(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(
            -Math.sin(yawRad) * cosPitch,
            -Math.sin(pitchRad),
            Math.cos(yawRad) * cosPitch
        );
    }

    public static float[] anglesFromVector(Vec3 vec) {
        double len = vec.length();
        if (len == 0) return new float[]{0, 0};
        Vec3 n = vec.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-n.x, n.z));
        float pitch = (float) Math.toDegrees(-Math.asin(n.y));
        return new float[]{yaw, pitch};
    }

    public static float normalizeYaw(float yaw) {
        yaw %= 360;
        if (yaw > 180) yaw -= 360;
        if (yaw < -180) yaw += 360;
        return yaw;
    }

    public static float clampPitch(float pitch) {
        return Math.clamp(pitch, -90, 90);
    }

    public static float diffYaw(float from, float to) {
        return normalizeYaw(to - from);
    }

    public static float diffPitch(float from, float to) {
        return to - from;
    }

    public static boolean isFacing(Entity entity, Vec3 target, float fov) {
        float[] angles = anglesTo(entity, target);
        float yawDiff = Math.abs(normalizeYaw(angles[0] - entity.getYRot()));
        float pitchDiff = Math.abs(angles[1] - entity.getXRot());
        return yawDiff <= fov && pitchDiff <= fov;
    }

    public static boolean isFacing(Entity entity, Entity target, float fov) {
        return isFacing(entity, target.position(), fov);
    }
}
