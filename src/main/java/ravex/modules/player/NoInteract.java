package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class NoInteract extends Module {
    public final BooleanParameter allBlocks = new BooleanParameter("AllBlocks", false);
    public final BooleanParameter chests = new BooleanParameter("Chests", true);
    public final BooleanParameter enderChests = new BooleanParameter("EnderChests", true);
    public final BooleanParameter furnaces = new BooleanParameter("Furnaces", true);
    public final BooleanParameter crafting = new BooleanParameter("Crafting", false);
    public final BooleanParameter enchanting = new BooleanParameter("Enchanting", false);

    public NoInteract() {
        chests.setVisible(() -> !allBlocks.getValue());
        enderChests.setVisible(() -> !allBlocks.getValue());
        furnaces.setVisible(() -> !allBlocks.getValue());
        crafting.setVisible(() -> !allBlocks.getValue());
        enchanting.setVisible(() -> !allBlocks.getValue());
    }

    public boolean shouldBlockAll() {
        return getEnabled() && allBlocks.getValue();
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(NoInteract.class);
    }
    public static NoInteract itz() {
        return ModuleManager.get(NoInteract.class);
    }
}
