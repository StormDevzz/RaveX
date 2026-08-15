package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "GhostHand", category = "Player")
public class GhostHand {
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




}