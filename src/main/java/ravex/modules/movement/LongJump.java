package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import net.minecraft.client.Minecraft;
import ravex.utility.misc.PhysicUtility;
import java.util.List;
@ModuleInfo(name = "LongJump", category = "Movement")
public class LongJump implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Vanilla"})
    public String mode = "Vanilla";
    @Parameter(name = "Boost", min = 1.0, max = 10.0, step = 0.1)
    public double boost = 1.5;
    public static boolean jumped = false;

    private LongJump() {
        
    }
    public void onEnable() {
        jumped = false;
    }
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
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

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("LongJump").getEnabled();
    }
    public static LongJump itz() {
        return ravex.manager.ModuleManager.delegate(LongJump.class);
    }


}