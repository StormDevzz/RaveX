package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class GhostHand extends Module {
<<<<<<< HEAD
=======
    public static final GhostHand INSTANCE = new GhostHand();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter range = new NumberParameter("Range", 6.0, 3.0, 12.0, 0.5);
    public final BooleanParameter chests = new BooleanParameter("Chests", true);
    public final BooleanParameter enderChests = new BooleanParameter("EnderChests", true);
    public final BooleanParameter furnaces = new BooleanParameter("Furnaces", true);
    public final BooleanParameter craftingTables = new BooleanParameter("Crafting", true);
    public final BooleanParameter enchantTables = new BooleanParameter("Enchanting", true);
    public final BooleanParameter allBlocks = new BooleanParameter("AllBlocks", false);
<<<<<<< HEAD
    public static boolean maybeEnabled() {
        return maybeEnabled(GhostHand.class);
    }
    public static GhostHand itz() {
        return ModuleManager.get(GhostHand.class);
    }
=======

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
