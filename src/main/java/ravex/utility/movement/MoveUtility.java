package ravex.utility.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import ravex.mcwrapper.MinecraftWrapper;

public class MoveUtility {
    public static void sendPos(double x, double y, double z, boolean onGround, boolean hCollision) {
        var c = MinecraftWrapper.getWrapper().getConnection();
        if (c != null)
            c.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(x, y, z, onGround, hCollision));
    }

    public static void sendRot(float yRot, float xRot, boolean onGround, boolean hCollision) {
        var c = MinecraftWrapper.getWrapper().getConnection();
        if (c != null)
            c.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yRot, xRot, onGround, hCollision));
    }

    public static void sendPosRot(double x, double y, double z, float yRot, float xRot, boolean onGround, boolean hCollision) {
        var c = MinecraftWrapper.getWrapper().getConnection();
        if (c != null)
            c.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot(x, y, z, yRot, xRot, onGround, hCollision));
    }

    public static void setMotion(Vec3 motion) {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        if (p != null) p.setDeltaMovement(motion);
    }

    public static void setMotion(double x, double y, double z) {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        if (p != null) p.setDeltaMovement(x, y, z);
    }

    public static void setPos(double x, double y, double z) {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        if (p != null) p.setPos(x, y, z);
    }

    public static Vec3 centerOf(BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    public static Vec3 eyePos() {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        return p != null ? p.getEyePosition(1.0F) : Vec3.ZERO;
    }

    public static Vec3 lookVec() {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        return p != null ? p.getViewVector(1.0F) : Vec3.ZERO;
    }

    public static double distance(Vec3 from, Vec3 to) {
        return from.distanceTo(to);
    }

    public static Vec3 scale(Vec3 v, double factor) {
        return v.multiply(factor, factor, factor);
    }

    public static Vec3 targetFromHit(Object hitResult, double heightOffset) {
        if (hitResult instanceof net.minecraft.world.phys.BlockHitResult bhr) {
            return centerOf(bhr.getBlockPos()).add(0, 0.5 + heightOffset, 0);
        }
        if (hitResult instanceof net.minecraft.world.phys.EntityHitResult ehr) {
            return ehr.getEntity().position().add(0, heightOffset, 0);
        }
        return null;
    }

    public static Vec3 getMotion() {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        return p != null ? p.getDeltaMovement() : Vec3.ZERO;
    }

    public static boolean isMoving() {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        if (p == null) return false;
        var input = p.input.keyPresses;
        return input != null && (input.forward() || input.backward() || input.left() || input.right());
    }

    public static boolean horizontalCollision() {
        var p = MinecraftWrapper.getWrapper().getPlayer();
        return p != null && p.horizontalCollision;
    }
}
