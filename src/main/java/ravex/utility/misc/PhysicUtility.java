package ravex.utility.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

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
}
