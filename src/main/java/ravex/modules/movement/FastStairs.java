package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.movement.MoveUtility;
import java.util.List;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "FastStairs", category = "Movement")
public class FastStairs {
    @Parameter(name = "Mode", modes = {"Simple", "Boost"})
    public String mode = "Simple";
    @Parameter(name = "Speed", min = 1.0, max = 5.0, step = 0.1)
    public double speed = 1.5;
    public static double calculateClimbSpeed(String mode, double currentY, double speedFactor) {
        double baseSpeed = (currentY > 0.0) ? currentY : 0.15;
        if ("Boost".equals(mode)) {
            return baseSpeed * speedFactor * 1.35;
        }
        return baseSpeed * speedFactor;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.player.onClimbable()) {
            double currentY = mc.player.getDeltaMovement().y;
            if (currentY > 0 && (mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown())) {
                double newY = calculateClimbSpeed(mode, currentY, speed);
                MoveUtility.setMotion(mc.player.getDeltaMovement().x, newY, mc.player.getDeltaMovement().z);
            }
        }
    }



}