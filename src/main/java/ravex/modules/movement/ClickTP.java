package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import ravex.utility.misc.PhysicUtility;

import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
@ModuleInfo(name = "ClickTP", category = "Movement")
public class ClickTP extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Instant", List.of("Instant", "Blink"));
    public final NumberParameter range = new NumberParameter("Range", 50.0, 10.0, 200.0, 5.0);
    public final NumberParameter cooldown = new NumberParameter("Cooldown", 500, 100, 2000, 50);
    private long lastClick = 0;
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyUse.isDown()) return;
        long now = System.currentTimeMillis();
        if (now - lastClick < cooldown.getValue().longValue()) return;
        lastClick = now;
        net.minecraft.world.phys.Vec3 target = getTarget(mc);
        if (target == null) return;
        if ("Instant".equals(mode.getValue())) {
            teleportInstant(mc, target);
        } else {
            teleportBlink(mc, target);
        }
    }
    private net.minecraft.world.phys.Vec3 getTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit != null) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
                return net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.5, 0);
            }
            if (hit.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                return entityHit.getEntity().position();
            }
        }
        double dist = range.getValue();
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = mc.player.getViewVector(1.0F);
        return eye.add(look.x * dist, look.y * dist, look.z * dist);
    }
    private void teleportInstant(Minecraft mc, net.minecraft.world.phys.Vec3 target) {
        var p = mc.player;
        p.connection.send(new ServerboundMovePlayerPacket.Pos(
                target.x, target.y, target.z, true, p.horizontalCollision));
        p.setPos(target.x, target.y, target.z);
    }
    private void teleportBlink(Minecraft mc, net.minecraft.world.phys.Vec3 target) {
        var p = mc.player;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double dx = (target.x - x) / 10.0;
        double dy = (target.y - y) / 10.0;
        double dz = (target.z - z) / 10.0;
        for (int i = 0; i < 10; i++) {
            x += dx;
            y += dy;
            z += dz;
            p.connection.send(new ServerboundMovePlayerPacket.Pos(
                    x, y, z, true, p.horizontalCollision));
        }
        p.setPos(target.x, target.y, target.z);
    }
    public static ClickTP itz() {
        return ravex.manager.ModuleManager.delegate(ClickTP.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}