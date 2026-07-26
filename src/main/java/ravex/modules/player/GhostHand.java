package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "GhostHand", category = "net.minecraft.world.entity.player.Player")
public class GhostHand implements ModuleAccess {
    @Parameter(name = "Range", min = 3.0, max = 12.0, step = 0.5)
    public double range = 6.0;
    @Parameter(name = "Chests")
    public boolean chests = true;
    @Parameter(name = "EnderChests")
    public boolean enderChests = true;
    @Parameter(name = "Furnaces")
    public boolean furnaces = true;
    @Parameter(name = "Crafting")
    public boolean craftingTables = true;
    @Parameter(name = "Enchanting")
    public boolean enchantTables = true;
    @Parameter(name = "AllBlocks")
    public boolean allBlocks = false;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("GhostHand").getEnabled();
    }
    public static GhostHand itz() {
        return ravex.manager.ModuleManager.delegate(GhostHand.class);
    }


}