package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "NoInteract", category = "net.minecraft.world.entity.player.Player")
public class NoInteract implements ModuleAccess {
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

    public NoInteract() {
    }

    public boolean shouldBlockAll() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoInteract").getEnabled() && allBlocks;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoInteract").getEnabled();
    }
    public static NoInteract itz() {
        return ravex.manager.ModuleManager.delegate(NoInteract.class);
    }


}