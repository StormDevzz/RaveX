package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "RidingHelper", category = "Movement")
public class RidingHelper {
    @Parameter(name = "Mode", modes = {"Normal", "Custom"})
    public String mode = "Normal";
    @Parameter(name = "Speed", min = 1.0, max = 5.0, step = 0.1)
    public double speed = 2.0;
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;
        var vehicle = player.getVehicle();
        if (vehicle == null) return;
        double mult = mode.equals("Custom") ? speed : 2.0;
        var motion = vehicle.getDeltaMovement();
        vehicle.setDeltaMovement(motion.x * mult, motion.y, motion.z * mult);
    }
}
