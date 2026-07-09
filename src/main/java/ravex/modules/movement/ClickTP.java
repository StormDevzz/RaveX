package ravex.modules.movement;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class ClickTP extends Module {
<<<<<<< HEAD
=======
    public static final ClickTP INSTANCE = new ClickTP();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Instant", List.of("Instant", "Blink"));
    public final NumberParameter range = new NumberParameter("Range", 50.0, 10.0, 200.0, 5.0);
    public final NumberParameter cooldown = new NumberParameter("Cooldown", 500, 100, 2000, 50);
    private long lastClick = 0;

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (!mc.options.keyUse.isDown()) return;
        long now = System.currentTimeMillis();
        if (now - lastClick < cooldown.getValue().longValue()) return;
        lastClick = now;
        Vec3 target = getTarget(mc);
        if (target == null) return;
        if ("Instant".equals(mode.getValue())) {
            teleportInstant(mc, target);
        } else {
            teleportBlink(mc, target);
        }
    }
    private Vec3 getTarget(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit != null) {
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos pos = blockHit.getBlockPos();
                return Vec3.atCenterOf(pos).add(0, 0.5, 0);
            }
            if (hit.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hit;
                return entityHit.getEntity().position();
            }
        }
        double dist = range.getValue();
        Vec3 eye = mc.player.getEyePosition(1.0F);
        Vec3 look = mc.player.getViewVector(1.0F);
        return eye.add(look.x * dist, look.y * dist, look.z * dist);
    }
    private void teleportInstant(Minecraft mc, Vec3 target) {
        var p = mc.player;
        p.connection.send(new ServerboundMovePlayerPacket.Pos(
                target.x, target.y, target.z, true, p.horizontalCollision));
        p.setPos(target.x, target.y, target.z);
    }
    private void teleportBlink(Minecraft mc, Vec3 target) {
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
<<<<<<< HEAD
    public static ClickTP itz() {
        return ModuleManager.get(ClickTP.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
