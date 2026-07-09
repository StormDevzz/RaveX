package ravex.modules.movement;
import net.minecraft.client.gui.screens.Screen;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import java.util.List;
public class GuiWalk extends Module {
    public static final GuiWalk INSTANCE = new GuiWalk();
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla", List.of("Vanilla", "NoClick", "NCPStrict"));
    public final BooleanParameter sneak = new BooleanParameter("Sneak", false);
    public final BooleanParameter noJump = new BooleanParameter("NoJump", false);
    public final BooleanParameter noSprint = new BooleanParameter("NoSprint", false);
    public Screen closedScreen = null;
}
