package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.PhysicUtility;
import java.util.List;
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
        var mc = MinecraftWrapper.getInstance();
        if (mc.player == null) return;

        if (mc.player.onGround()) {
            jumped = false;
        } else if (!jumped) {
            double speed = boost;
            net.minecraft.world.phys.Vec3 motion = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(motion.x * speed, motion.y + 0.05, motion.z * speed);
            jumped = true;
        }
    }





}