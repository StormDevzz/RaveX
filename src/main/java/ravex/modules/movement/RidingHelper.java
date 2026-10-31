package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "RidingHelper", category = "Movement")
public class RidingHelper {
    @Parameter(name = "Mode", modes = {"Vanilla", "Custom"})
    public String mode = "Vanilla";
    // horses get out of hand above this
    @Parameter(name = "Speed", min = 1.0, max = 1.5, step = 0.05, visible = "mode=Vanilla")
    public double vanillaSpeed = 1.2;
    @Parameter(name = "Horizontal", min = 1.0, max = 3.0, step = 0.05, visible = "mode=Custom")
    public double customHorizontal = 1.3;
    @Parameter(name = "Vertical", min = 0.0, max = 2.0, step = 0.05, visible = "mode=Custom")
    public double customVertical = 1.0;
    @Parameter(name = "Jump", visible = "mode=Custom")
    public boolean customJump = true;
    @Parameter(name = "JumpPower", min = 0.1, max = 1.0, step = 0.05, visible = "mode=Custom && customJump")
    public double customJumpPower = 0.35;
    @Parameter(name = "Smooth", visible = "mode=Custom")
    public boolean customSmooth = true;
    @Parameter(name = "SmoothFactor", min = 0.1, max = 1.0, step = 0.05, visible = "mode=Custom && customSmooth")
    public double customSmoothFactor = 0.6;
    @Parameter(name = "OnlyForward", visible = "mode=Custom")
    public boolean customOnlyForward = false;
    @Parameter(name = "OnlyOnGround", visible = "mode=Custom")
    public boolean customOnlyOnGround = false;

    private double smoothX, smoothZ;
    private boolean hasSmooth;

    public void onDisable() {
        hasSmooth = false;
        smoothX = 0;
        smoothZ = 0;
    }

    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        var vehicle = player.getVehicle();
        if (vehicle == null) {
            hasSmooth = false;
            return;
        }
        var motion = vehicle.getDeltaMovement();

        if ("Vanilla".equals(mode)) {
            vehicle.setDeltaMovement(motion.x * vanillaSpeed, motion.y, motion.z * vanillaSpeed);
            return;
        }

        boolean forward = mc.getOptions() != null && mc.getOptions().keyUp.isDown();
        if (customOnlyForward && !forward) {
            vehicle.setDeltaMovement(motion.x, motion.y, motion.z);
            return;
        }
        if (customOnlyOnGround && !vehicle.onGround()) {
            vehicle.setDeltaMovement(motion.x, motion.y, motion.z);
            return;
        }

        double hx = motion.x * customHorizontal;
        double hz = motion.z * customHorizontal;
        double vy = motion.y * customVertical;

        if (customJump && mc.getOptions() != null && mc.getOptions().keyJump.isDown() && vehicle.onGround()) {
            vy = customJumpPower;
        }

        if (customSmooth) {
            if (!hasSmooth) {
                smoothX = hx;
                smoothZ = hz;
                hasSmooth = true;
            } else {
                smoothX += (hx - smoothX) * customSmoothFactor;
                smoothZ += (hz - smoothZ) * customSmoothFactor;
            }
            hx = smoothX;
            hz = smoothZ;
        }

        vehicle.setDeltaMovement(hx, vy, hz);
    }
}
