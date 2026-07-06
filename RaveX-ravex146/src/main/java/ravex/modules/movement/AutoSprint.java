package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import net.minecraft.client.Minecraft;
import java.util.List;

public class AutoSprint extends Module {
    public static final AutoSprint INSTANCE = new AutoSprint();

    public final ModeParameter mode = new ModeParameter("Mode", "Rage", List.of("Legit", "Rage"));

    private AutoSprint() {
        super("AutoSprint", Category.MOVEMENT);
        addParameter(mode);
    }

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
}
