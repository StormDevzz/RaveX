package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;

/**
 * NoInteract — prevents the player from accidentally opening containers
 * (chests, barrels, droppers, etc.) and other block interactions.
 *
 * The mixin MixinNoInteract hooks into Minecraft.gameMode.useItemOn()
 * and cancels the right-click packet when the targeted block is a container.
 */
public class NoInteract extends Module {
    public static final NoInteract INSTANCE = new NoInteract();

    public final BooleanParameter containers = new BooleanParameter("Containers", true);
    public final BooleanParameter craftingTables = new BooleanParameter("Crafting Tables", false);
    public final BooleanParameter buttons = new BooleanParameter("Buttons/Levers", false);

    private NoInteract() {
        super("NoInteract", Category.PLAYER);
        addParameter(containers);
        addParameter(craftingTables);
        addParameter(buttons);
    }
}
