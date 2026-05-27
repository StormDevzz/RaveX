package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;

/**
 * GuiWalk — allows the player to move while a GUI screen is open.
 * The mixin MixinGuiWalk hooks into Screen's shouldCloseOnEsc / input handling
 * and lets movement keys pass through when this module is enabled.
 */
public class GuiWalk extends Module {
    public static final GuiWalk INSTANCE = new GuiWalk();

    private GuiWalk() {
        super("GuiWalk", Category.MOVEMENT);
    }
}
