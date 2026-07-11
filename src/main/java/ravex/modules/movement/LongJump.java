package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import java.util.List;
public class LongJump extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla"));
    public final NumberParameter boost = new NumberParameter("Boost", 1.5, 1.0, 10.0, 0.1);
    public static boolean jumped = false;

    private LongJump() {
        super("LongJump");
    }

    @Override
    protected void onEnable() {
        jumped = false;
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (mc.player.onGround()) {
            jumped = false;
        } else if (!jumped) {
            double speed = boost.getValue();
            Vec3 motion = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(motion.x * speed, motion.y + 0.05, motion.z * speed);
            jumped = true;
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(LongJump.class);
    }
    public static LongJump itz() {
        return ModuleManager.get(LongJump.class);
    }
}
