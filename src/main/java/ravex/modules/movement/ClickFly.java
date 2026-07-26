package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.block.BlockUtility;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import ravex.utility.misc.PhysicUtility;

import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
@ModuleInfo(name = "ClickFly", category = "Movement")
public class ClickFly extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Fly", List.of("Fly", "TP"));
    public final NumberParameter speed = new NumberParameter("Speed", 1.5, 0.5, 5.0, 0.25);
    public final NumberParameter range = new NumberParameter("Range", 100.0, 10.0, 300.0, 10.0);
    public final NumberParameter height = new NumberParameter("Height", 0.0, -5.0, 10.0, 0.5);
    public final BooleanParameter autoLand = new BooleanParameter("AutoLand", true);
    private net.minecraft.world.phys.Vec3 target = null;
    private boolean flying = false;
    private long lastClick = 0;
    protected void onEnable() {
        target = null;
        flying = false;
    }
    protected void onDisable() {
        target = null;
        flying = false;
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.keyUse.isDown()) {
            long now = System.currentTimeMillis();
            if (now - lastClick > 300) {
                lastClick = now;
                net.minecraft.world.phys.Vec3 newTarget = getTarget(mc);
                if (newTarget != null) {
                    target = newTarget;
                    flying = true;
                }
            }
        }
        if (!flying || target == null) return;
        if ("TP".equals(mode.getValue())) {
            tpStep(mc);
        } else {
            flyStep(mc);
        }
    }
    private net.minecraft.world.phys.Vec3 getTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit != null) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
                return net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.5 + height.getValue(), 0);
            }
            if (hit.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                return entityHit.getEntity().position().add(0, height.getValue(), 0);
            }
        }
        double dist = range.getValue();
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = mc.player.getViewVector(1.0F);
        return eye.add(look.x * dist, look.y * dist + height.getValue(), look.z * dist);
    }
    private void flyStep(Minecraft mc) {
        var p = mc.player;
        net.minecraft.world.phys.Vec3 pos = p.position();
        net.minecraft.world.phys.Vec3 diff = target.subtract(pos);
        double dist = diff.length();
        if (dist < 1.5) {
            if (autoLand.getValue()) {
                p.setDeltaMovement(0, 0, 0);
                flying = false;
                target = null;
            }
            return;
        }
        net.minecraft.world.phys.Vec3 dir = diff.normalize();
        double spd = speed.getValue();
        p.setDeltaMovement(dir.x * spd, dir.y * spd, dir.z * spd);
        p.connection.send(new ServerboundMovePlayerPacket.Pos(
                pos.x + dir.x * spd,
                pos.y + dir.y * spd,
                pos.z + dir.z * spd,
                false, p.horizontalCollision));
    }
    private void tpStep(Minecraft mc) {
        var p = mc.player;
        net.minecraft.world.phys.Vec3 pos = p.position();
        net.minecraft.world.phys.Vec3 diff = target.subtract(pos);
        double dist = diff.length();
        if (dist < 1.5) {
            flying = false;
            target = null;
            return;
        }
        net.minecraft.world.phys.Vec3 dir = diff.normalize();
        double spd = speed.getValue();
        double step = Math.min(spd, dist);
        net.minecraft.world.phys.Vec3 next = pos.add(dir.x * step, dir.y * step, dir.z * step);
        p.connection.send(new ServerboundMovePlayerPacket.Pos(
                next.x, next.y, next.z, true, p.horizontalCollision));
        p.setPos(next.x, next.y, next.z);
    }
    public static ClickFly itz() {
        return ravex.manager.ModuleManager.delegate(ClickFly.class);
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