package ravex.modules.misc;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "AntiAfk", category = "Misc")
public class AntiAfk {
    @Parameter(name = "Spin")
    public boolean spin = true;
    @Parameter(name = "SpinSpeed", min = 5.0, max = 180.0, step = 5.0, visible = "spin")
    public double spinSpeed = 45.0;
    @Parameter(name = "SpinDelay", min = 50, max = 1000, step = 50, visible = "spin")
    public double spinDelay = 50;
    @Parameter(name = "Jump")
    public boolean jump = true;
    @Parameter(name = "JumpDelay", min = 1000, max = 60000, step = 500, visible = "jump")
    public double jumpDelay = 12000;
    @Parameter(name = "Sneak")
    public boolean sneak = false;
    @Parameter(name = "SneakDelay", min = 1000, max = 60000, step = 500, visible = "sneak")
    public double sneakDelay = 12000;
    @Parameter(name = "LookAround")
    public boolean lookAround = true;
    @Parameter(name = "LookDelay", min = 1000, max = 60000, step = 500, visible = "lookAround")
    public double lookDelay = 12000;
    @Parameter(name = "Rotation", min = 10.0, max = 180.0, step = 5.0, visible = "lookAround")
    public double rotationRange = 45.0;
    private long lastSpinAt = 0;
    private long nextJumpAt = 0;
    private long nextSneakAt = 0;
    private long nextLookAt = 0;
    private long sneakUntil = 0;

    public void onEnable() {
        long now = System.currentTimeMillis();
        lastSpinAt = now;
        nextJumpAt = now;
        nextSneakAt = now;
        nextLookAt = now;
        sneakUntil = 0;
    }

    public void onDisable() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getOptions() != null) mc.getOptions().keyShift.setDown(false);
    }

    private long jittered(long delayMs) {
        long jittered = delayMs + (long) ((Math.random() - 0.5) * delayMs * 0.6);
        return Math.max(50L, jittered);
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null || mc.getLevel() == null) return;
        long now = System.currentTimeMillis();
        if (spin && now - lastSpinAt >= (long) spinDelay) {
            double elapsedSec = Math.min(1.0, (now - lastSpinAt) / 1000.0);
            player.setYRot(player.getYRot() + (float) (spinSpeed * elapsedSec));
            lastSpinAt = now;
        }
        if (sneakUntil != 0 && now >= sneakUntil) {
            mc.getOptions().keyShift.setDown(false);
            sneakUntil = 0;
        }
        if (lookAround && now >= nextLookAt) {
            player.setYRot(player.getYRot() + (float) ((Math.random() * 2.0 - 1.0) * rotationRange));
            float pitch = player.getXRot() + (float) ((Math.random() * 2.0 - 1.0) * rotationRange * 0.5);
            if (pitch > 90f) pitch = 90f;
            if (pitch < -90f) pitch = -90f;
            player.setXRot(pitch);
            nextLookAt = now + jittered((long) lookDelay);
        }
        if (jump && now >= nextJumpAt) {
            if (player.onGround()) player.jumpFromGround();
            nextJumpAt = now + jittered((long) jumpDelay);
        }
        if (sneak && now >= nextSneakAt) {
            mc.getOptions().keyShift.setDown(true);
            sneakUntil = now + 400 + (long) (Math.random() * 400);
            nextSneakAt = now + jittered((long) sneakDelay);
        }
    }
}
