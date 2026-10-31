package ravex.utility.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.utility.movement.MoveUtility;

public class PhysicUtility {
    public static Vec3 centerOf(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    public static Vec3 atCenterOf(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    public static double distanceToSqr(Vec3 a, Vec3 b) {
        return a.distanceToSqr(b);
    }

    public static double distanceToSqr(Vec3 a, double x, double y, double z) {
        return a.distanceToSqr(x, y, z);
    }

    public static Vec3 vec3(double x, double y, double z) {
        return new Vec3(x, y, z);
    }

    public static BlockHitResult raycast(Vec3 from, Vec3 to) {
        var mc = MinecraftWrapper.getWrapper();
        var level = mc.getLevel();
        var player = mc.getPlayer();
        if (level == null || player == null) return null;
        var hit = level.clip(new net.minecraft.world.level.ClipContext(
            from, to,
            net.minecraft.world.level.ClipContext.Block.OUTLINE,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            player));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK ? hit : null;
    }

    public static BlockHitResult raycastFromEyes(double range) {
        Vec3 eye = MoveUtility.eyePos();
        Vec3 look = MoveUtility.lookVec();
        Vec3 end = eye.add(look.x * range, look.y * range, look.z * range);
        return raycast(eye, end);
    }
}
