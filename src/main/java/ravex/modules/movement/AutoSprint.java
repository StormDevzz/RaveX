package ravex.modules.movement;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
public class AutoSprint extends Module {
<<<<<<< HEAD
=======
    public static final AutoSprint INSTANCE = new AutoSprint();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Rage", List.of("Legit", "Rage"));

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if ("Rage".equals(mode.getValue())) {
            mc.player.setSprinting(true);
        } else {
            if (mc.player.input.hasForwardImpulse() && !mc.player.isUsingItem() && !mc.player.isShiftKeyDown()) {
                mc.player.setSprinting(true);
            }
        }
    }
    public static AutoSprint itz() {
        return ModuleManager.get(AutoSprint.class);
    }
}
