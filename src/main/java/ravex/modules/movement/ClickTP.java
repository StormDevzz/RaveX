package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.movement.MoveUtility;
import ravex.utility.network.NetworkUtility;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "ClickTP", category = "Movement")
public class ClickTP {
    @Parameter(name = "Mode", modes = {"Instant", "Blink"})
    public String mode = "Instant";
    @Parameter(name = "Range", min = 10.0, max = 200.0, step = 5.0)
    public double range = 50.0;
    @Parameter(name = "Cooldown", min = 0, max = 2000, step = 50)
    public double cooldown = 100;
    private long lastClick = 0;
    private boolean lastUseDown = false;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        // fire on the press edge only
        boolean useDown = mc.getOptions().keyUse.isDown();
        boolean pressed = useDown && !lastUseDown;
        lastUseDown = useDown;
        if (!pressed) return;
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
        var target = MoveUtility.targetFromHit(mc.getHitResult(), 0.5, 0.0);
        if (target != null) return target;
        // only onto a real block along the look ray, never the void
        var clip = PhysicUtility.raycastFromEyes(range);
        if (clip == null) return null;
        return PhysicUtility.centerOf(clip.getBlockPos()).add(0, 0.5, 0);
    }
    private void teleportInstant(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        var p = mc.getPlayer();
        NetworkUtility.sendMoveRelative(target.x, target.y, target.z, true, p.horizontalCollision);
        p.setPos(target.x, target.y, target.z);
    }
    private void teleportBlink(MinecraftWrapper mc, net.minecraft.world.phys.Vec3 target) {
        var p = mc.getPlayer();
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double dx = target.x - x;
        double dy = target.y - y;
        double dz = target.z - z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        // small steps so the server can follow the path
        int steps = Math.max(1, Math.min(20, (int) Math.ceil(dist)));
        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            NetworkUtility.sendMoveRelative(x + dx * t, y + dy * t, z + dz * t, true, p.horizontalCollision);
        }
        p.setPos(target.x, target.y, target.z);
    }



}