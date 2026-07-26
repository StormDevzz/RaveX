package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import java.util.List;




@ModuleInfo(name = "ClickFly", category = "Movement")
public class ClickFly implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Fly", "TP"})
    public String mode = "Fly";
    @Parameter(name = "Speed", min = 0.5, max = 5.0, step = 0.25)
    public double speed = 1.5;
    @Parameter(name = "Range", min = 10.0, max = 300.0, step = 10.0)
    public double range = 100.0;
    @Parameter(name = "Height", min = -5.0, max = 10.0, step = 0.5)
    public double height = 0.0;
    @Parameter(name = "AutoLand")
    public boolean autoLand = true;
    private net.minecraft.world.phys.Vec3 target = null;
    private boolean flying = false;
    private long lastClick = 0;
    public void onEnable() {
        target = null;
        flying = false;
    }
    public void onDisable() {
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
        if ("TP".equals(mode)) {
            tpStep(mc);
        } else {
            flyStep(mc);
        }
    }
    private net.minecraft.world.phys.Vec3 getTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit != null) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) hit;
                net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
                return net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.5 + height, 0);
            }
            if (hit.getType() == HitResult.Type.ENTITY) {
                net.minecraft.world.phys.EntityHitResult entityHit = (net.minecraft.world.phys.EntityHitResult) hit;
                return entityHit.getEntity().position().add(0, height, 0);
            }
        }
        double dist = range;
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = mc.player.getViewVector(1.0F);
        return eye.add(look.x * dist, look.y * dist + height, look.z * dist);
    }
    private void flyStep(Minecraft mc) {
        var p = mc.player;
        net.minecraft.world.phys.Vec3 pos = p.position();
        net.minecraft.world.phys.Vec3 diff = target.subtract(pos);
        double dist = diff.length();
        if (dist < 1.5) {
            if (autoLand) {
                p.setDeltaMovement(0, 0, 0);
                flying = false;
                target = null;
            }
            return;
        }
        net.minecraft.world.phys.Vec3 dir = diff.normalize();
        double spd = speed;
        p.setDeltaMovement(dir.x * spd, dir.y * spd, dir.z * spd);
        p.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
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
        double spd = speed;
        double step = Math.min(spd, dist);
        net.minecraft.world.phys.Vec3 next = pos.add(dir.x * step, dir.y * step, dir.z * step);
        p.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos(
                next.x, next.y, next.z, true, p.horizontalCollision));
        p.setPos(next.x, next.y, next.z);
    }
    public static ClickFly itz() {
        return ravex.manager.ModuleManager.delegate(ClickFly.class);
    }


}