package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import java.util.List;
public class AutoSprint extends Module {
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
