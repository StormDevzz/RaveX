package ravex.modules.movement;
import ravex.utility.player.PlayerUtility;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.movement.MoveUtility;
import ravex.mcwrapper.MinecraftWrapper;
@Module(name = "LongJump", category = "Movement")
public class LongJump {
    @Parameter(name = "Mode", modes = {"Vanilla"})
    public String mode = "Vanilla";
    @Parameter(name = "Boost", min = 1.0, max = 10.0, step = 0.1)
    public double boost = 1.5;
    public static boolean jumped = false;

    public void onEnable() {
        jumped = false;
    }
    public void onTick() {
        var mc = MinecraftWrapper.getWrapper();
        var player = mc.getPlayer();
        if (player == null) return;

        if (PlayerUtility.isOnGround()) {
            jumped = false;
        } else if (!jumped) {
            double speed = boost;
            var motion = PlayerUtility.getDeltaMovement();
            MoveUtility.setMotion(motion.x * speed, motion.y + 0.05, motion.z * speed);
            jumped = true;
        }
    }
}
