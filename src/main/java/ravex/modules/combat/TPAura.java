package ravex.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;

import java.util.List;

public class TPAura extends Module {

    public final BooleanParameter swing = new BooleanParameter("Swing", true);
    public final NumberParameter attackDelay = new NumberParameter("AttackDelay", 5, 0, 20, 1);
    public final BooleanParameter rotateToTarget = new BooleanParameter("Rotate", false);
    public final NumberParameter packets = new NumberParameter("Packets", 4, 1, 5, 1);
    public final NumberParameter maxDistance = new NumberParameter("Distance", 22.0, 1.0, 22.0, 0.5);
    public final NumberParameter horizontalOffset = new NumberParameter("H-Offset", 0.05, 0.001, 0.99, 0.001);
    public final NumberParameter yOffset = new NumberParameter("Y-Offset", 0.01, 0.001, 0.99, 0.001);

    private int attackTicks = 0;

    public TPAura() {
        super("TPAura");
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(TPAura.class);
    }

    public static TPAura itz() {
        return ModuleManager.get(TPAura.class);
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

        attackTicks++;
        if (attackTicks > attackDelay.getValue().intValue()) {
            hitEntity(mc);
            attackTicks = 0;
        }
    }

    private Entity findClosestTarget(Minecraft mc) {
        Entity best = null;
        double bestDist = Double.MAX_VALUE;
        double maxDistSq = maxDistance.getValue() * maxDistance.getValue();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (!entity.isAlive()) continue;
            if (!entity.isAttackable()) continue;
            if (entity.isInvulnerable()) continue;
            if (!(entity.getType() == EntityType.PLAYER)) continue;
            if (entity.distanceToSqr(mc.player) > maxDistSq) continue;

            double dist = entity.distanceToSqr(mc.player);
            if (dist < bestDist) {
                bestDist = dist;
                best = entity;
            }
        }
        return best;
    }

    private boolean isValidTarget(Entity entity, Minecraft mc) {
        Entity self = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;
        return entity.isAlive() && self.distanceTo(entity) <= maxDistance.getValue();
    }

    private void hitEntity(Minecraft mc) {
        if (mc.player == null || mc.getConnection() == null) return;
        Entity self = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;
        Entity target = findClosestTarget(mc);

        if (target == null || !isValidTarget(target, mc)) {
            attackTicks = 0;
            return;
        }

        Vec3 startPos = self.position();
        Vec3 targetPos = target.position();

        double actualDist = startPos.distanceTo(targetPos);
        if (actualDist > maxDistance.getValue() - 0.5) return;

        double yOffsetVal = mc.player.getVehicle() != null
                ? target.getBoundingBox().maxY + 0.3
                : target.getBoundingBox().getCenter().y;

        Vec3 insideTarget = new Vec3(targetPos.x, yOffsetVal, targetPos.z);
        Vec3 finalPos = !invalid(insideTarget, mc) ? insideTarget : findNearestPos(insideTarget, mc);
        if (finalPos == null) return;

        Vec3 highPos = startPos.add(0, maxDistance.getValue(), 0);
        if (invalid(finalPos, mc) || invalid(highPos, mc)) return;

        int amountOfPackets = packets.getValue().intValue();
        for (int i = 0; i < amountOfPackets; i++) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                    mc.player.getYRot(), mc.player.getXRot(), false, mc.player.horizontalCollision));
        }

        sendMove(mc, highPos);
        sendMove(mc, finalPos);

        if (rotateToTarget.getValue()) {
            Vec3 toTarget = target.getBoundingBox().getCenter().subtract(mc.player.getEyePosition()).normalize();
            float yaw = (float) (Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
            float pitch = (float) -Math.toDegrees(Math.asin(Math.min(Math.max(toTarget.y, -1.0), 1.0)));
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, false, mc.player.horizontalCollision));
        }

        if (swing.getValue()) {
            mc.getConnection().send(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(target, false));

        sendMove(mc, highPos);
        sendMove(mc, startPos);

        Vec3 offset = getOffset(startPos, mc);
        sendMove(mc, offset);
        mc.player.setPos(offset.x, offset.y, offset.z);
    }

    private Vec3 findNearestPos(Vec3 desired, Minecraft mc) {
        if (!invalid(desired, mc)) return desired;

        Vec3 best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = -2; dy <= 2; dy++) {
                    Vec3 test = desired.add(dx, dy, dz);
                    if (invalid(test, mc)) continue;
                    double dist = test.distanceToSqr(desired);
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = test;
                    }
                }
            }
        }
        return best;
    }

    private void sendMove(Minecraft mc, Vec3 pos) {
        if (mc.getConnection() == null) return;
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                pos.x, pos.y, pos.z, false, mc.player.horizontalCollision));
    }

    private Vec3 getOffset(Vec3 base, Minecraft mc) {
        double dx = horizontalOffset.getValue();
        double dy = yOffset.getValue();

        Vec3[] offsets = {
                base.add(dx, dy, 0), base.add(-dx, dy, 0),
                base.add(0, dy, dx), base.add(0, dy, -dx),
                base.add(dx, dy, dx), base.add(-dx, dy, -dx),
                base.add(-dx, dy, dx), base.add(dx, dy, -dx)
        };

        for (Vec3 pos : offsets) {
            if (!invalid(pos, mc)) return pos;
        }

        Vec3 noHorizontal = base.add(0, dy, 0);
        if (!invalid(noHorizontal, mc)) return noHorizontal;
        return base;
    }

    private boolean invalid(Vec3 pos, Minecraft mc) {
        if (mc.level == null) return true;
        if (mc.level.getChunk(BlockPos.containing(pos)) == null) return true;

        Entity entity = mc.player.isPassenger() ? mc.player.getVehicle() : mc.player;
        AABB targetBox = entity.getBoundingBox().move(
                pos.x - entity.getX(), pos.y - entity.getY(), pos.z - entity.getZ());

        for (BlockPos bp : BlockPos.betweenClosed(
                BlockPos.containing(targetBox.minX, targetBox.minY, targetBox.minZ),
                BlockPos.containing(targetBox.maxX, targetBox.maxY, targetBox.maxZ))) {
            BlockState state = mc.level.getBlockState(bp);
            if (state.is(Blocks.LAVA) || !state.getCollisionShape(mc.level, bp).isEmpty()) {
                return true;
            }
        }

        for (Entity e : mc.level.getEntities(entity, targetBox)) {
            if (e.canBeCollidedWith(entity)) return true;
        }
        return false;
    }
}
