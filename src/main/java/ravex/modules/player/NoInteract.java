package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;
@Module(name = "NoInteract", category = "net.minecraft.world.entity.player.Player")
public class NoInteract {
    @Parameter(name = "AllBlocks")
    public boolean allBlocks = false;
    @Parameter(name = "Chests")
    public boolean chests = true;
    @Parameter(name = "EnderChests")
    public boolean enderChests = true;
    @Parameter(name = "Furnaces")
    public boolean furnaces = true;
    @Parameter(name = "Crafting")
    public boolean crafting = false;
    @Parameter(name = "Enchanting")
    public boolean enchanting = false;

    public boolean shouldBlockAll() {
        return Modules.enabled(NoInteract.class) && allBlocks;
    }




}