package ravex.modules.movement;
import net.minecraft.client.gui.screens.Screen;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import java.util.List;
public class GuiMove extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "NoClick", "NCPStrict", "Grim", "Matrix"));
    public final BooleanParameter sneak = new BooleanParameter("Sneak", false);
    public final BooleanParameter noJump = new BooleanParameter("NoJump", false);
    public final BooleanParameter noSprint = new BooleanParameter("NoSprint", false);
    public Screen closedScreen = null;
    public int grimCooldown = 0;
    public static boolean maybeEnabled() {
        return maybeEnabled(GuiMove.class);
    }
    public static GuiMove itz() {
        return ModuleManager.get(GuiMove.class);
    }
}