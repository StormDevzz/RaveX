package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.block.BlockUtility;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.network.NetworkUtility;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "ClickTP", category = "Movement")
public class ClickTP {
    @Parameter(name = "Mode", modes = {"Instant", "Blink"})
    public String mode = "Instant";
    @Parameter(name = "Range", min = 10.0, max = 200.0, step = 5.0)
    public double range = 50.0;
    @Parameter(name = "Cooldown", min = 100, max = 2000, step = 50)
    public double cooldown = 500;
    private long lastClick = 0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        if (!mc.getOptions().keyUse.isDown()) return;
        long now = System.currentTimeMillis();
        if (now - lastClick < (long) cooldown) return;
        lastClick = now;
        net.minecraft.world.phys.Vec3 target = getTarget(mc);
        if (target == null) return;
        if ("Instant".equals(mode)) {
            teleportInstant(mc, target);
        } else {
            teleportBlink(mc, target);
        }
    }
    private net.minecraft.world.phys.Vec3 getTarget(MinecraftWrapper mc) {
        HitResult hit = mc.getHitResult();
        if (hit != null) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                net.minecraft.world.phys.BlockHitResult blockHit = (net.minecraft.world.phys.BlockHitResult) hit;
                net.minecraft.core.BlockPos pos = blockHit.getBlockPos();
                return PhysicUtility.centerOf(pos).add(0, 0.5, 0);
            }
            if (hit.getType() == HitResult.Type.ENTITY) {
                net.minecraft.world.phys.EntityHitResult entityHit = (net.minecraft.world.phys.EntityHitResult) hit;
                return entityHit.getEntity().position();
            }
        }
        double dist = range;
        net.minecraft.world.phys.Vec3 eye = mc.getPlayer().getEyePosition(1.0F);
        net.minecraft.world.phys.Vec3 look = mc.getPlayer().getViewVector(1.0F);
        return eye.add(look.x * dist, look.y * dist, look.z * dist);
    }
    private void teleportInstant(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        var p = mc.getPlayer();
        NetworkUtility.sendMoveRelative(target.x, target.y, target.z, true, p.horizontalCollision);
        p.setPos(target.x, target.y, target.z);
    }
    private void teleportBlink(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        var p = mc.getPlayer();
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double dx = (target.x - x) / 10.0;
        double dy = (target.y - y) / 10.0;
        double dz = (target.z - z) / 10.0;
        for (int i = 0; i < 10; i++) {
            x += dx;
            y += dy;
            z += dz;
            NetworkUtility.sendMoveRelative(x, y, z, true, p.horizontalCollision);
        }
        p.setPos(target.x, target.y, target.z);
    }



}