package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;

public class GuiWalk extends Module {
    public static final GuiWalk INSTANCE = new GuiWalk();

    public final BooleanParameter sneak = new BooleanParameter("Sneak", false);

    private GuiWalk() {
        super("GuiWalk", Category.MOVEMENT);
        addParameter(sneak);
    }
}
