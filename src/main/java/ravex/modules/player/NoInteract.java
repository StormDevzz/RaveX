package ravex.modules.player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class NoInteract extends Module {
    public static final NoInteract INSTANCE = new NoInteract();
    public final BooleanParameter containers = new BooleanParameter("Containers", true);
    public final BooleanParameter craftingTables = new BooleanParameter("CraftingTables", false);
    public final BooleanParameter buttons = new BooleanParameter("Buttons/Levers", false);

}
