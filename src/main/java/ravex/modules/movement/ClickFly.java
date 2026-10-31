package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;




@Module(name = "ClickFly", category = "Movement")
public class ClickFly {
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
    private long stuckSince = 0;
    private double lastDist = Double.NaN;
    public void onEnable() {
        target = null;
        flying = false;
        lastClick = 0;
        resetProgress();
    }
    public void onDisable() {
        target = null;
        flying = false;
        resetProgress();
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getPlayer() == null || mc.getLevel() == null) return;
        boolean moving = MoveUtility.isMoving();
        if (flying && moving) {
            abort();
        }
        if (mc.getOptions().keyUse.isDown() && !moving) {
            long now = System.currentTimeMillis();
            if (now - lastClick > 300) {
                lastClick = now;
                net.minecraft.world.phys.Vec3 newTarget = getTarget(mc);
                if (newTarget != null) {
                    target = newTarget;
                    flying = true;
                    resetProgress();
                }
            }
        }
        if (!flying || target == null) return;
        flyStep(mc);
    }
    private void abort() {
        target = null;
        flying = false;
        resetProgress();
    }
    private void resetProgress() {
        lastDist = Double.NaN;
        stuckSince = 0;
    }
    private boolean isStuck(double dist) {
        long now = System.currentTimeMillis();
        if (Double.isNaN(lastDist) || dist < lastDist - 0.05) {
            stuckSince = now;
        }
        lastDist = dist;
        return now - stuckSince > 800;
    }
    private net.minecraft.world.phys.Vec3 getTarget(MinecraftWrapper mc) {
        var target = MoveUtility.targetFromHit(mc.getHitResult(), 1.0 + height, height);
        if (target != null) return target;
        // only a real block along the ray, never the void
        var clip = PhysicUtility.raycastFromEyes(range);
        if (clip == null) return null;
        return PhysicUtility.centerOf(clip.getBlockPos()).add(0, 1.0 + height, 0);
    }
    private void flyStep(MinecraftWrapper mc) {
        var p = mc.getPlayer();
        net.minecraft.world.phys.Vec3 pos = p.position();
        net.minecraft.world.phys.Vec3 diff = target.subtract(pos);
        double dist = diff.length();
        if (dist < 1.5) {
            if (autoLand) {
                MoveUtility.setMotion(0, 0, 0);
            }
            abort();
            return;
        }
        if (isStuck(dist)) {
            if (autoLand) {
                MoveUtility.setMotion(0, 0, 0);
            }
            abort();
            return;
        }
        net.minecraft.world.phys.Vec3 dir = diff.normalize();
        // steer up over obstacles instead of pressing in
        if (p.horizontalCollision || p.verticalCollision) {
            dir = new net.minecraft.world.phys.Vec3(dir.x, Math.max(dir.y, 0.45), dir.z).normalize();
        }
        double spd = speed;
        MoveUtility.setMotion(dir.x * spd, dir.y * spd, dir.z * spd);
    }
    private void tpStep(MinecraftWrapper mc) {
        abort();
    }
}
