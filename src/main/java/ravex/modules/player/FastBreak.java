package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "FastBreak", category = "net.minecraft.world.entity.player.Player")
public class FastBreak implements ModuleAccess {
    @Parameter(name = "Delay", min = 0, max = 4, step = 1)
    public double delay = 0;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("FastBreak").getEnabled();
    }
    public static FastBreak itz() {
        return ravex.manager.ModuleManager.delegate(FastBreak.class);
    }


}