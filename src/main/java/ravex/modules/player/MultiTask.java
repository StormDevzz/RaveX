package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
@ModuleInfo(name = "MultiTask", category = "net.minecraft.world.entity.player.Player")
public class MultiTask implements ModuleAccess {
public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MultiTask").getEnabled();
    }
    public static MultiTask itz() {
        return ravex.manager.ModuleManager.delegate(MultiTask.class);
    }


}