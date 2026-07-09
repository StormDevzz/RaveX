package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class GhostHand extends Module {
    public final NumberParameter range = new NumberParameter("Range", 6.0, 3.0, 12.0, 0.5);
    public final BooleanParameter chests = new BooleanParameter("Chests", true);
    public final BooleanParameter enderChests = new BooleanParameter("EnderChests", true);
    public final BooleanParameter furnaces = new BooleanParameter("Furnaces", true);
    public final BooleanParameter craftingTables = new BooleanParameter("Crafting", true);
    public final BooleanParameter enchantTables = new BooleanParameter("Enchanting", true);
    public final BooleanParameter allBlocks = new BooleanParameter("AllBlocks", false);
    public static boolean maybeEnabled() {
        return maybeEnabled(GhostHand.class);
    }
    public static GhostHand itz() {
        return ModuleManager.get(GhostHand.class);
    }
}
