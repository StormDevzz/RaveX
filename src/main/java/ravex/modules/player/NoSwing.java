package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "NoSwing", category = "net.minecraft.world.entity.player.Player")
public class NoSwing implements ModuleAccess {
    @Parameter(name = "Self")
    public boolean self = true;
    @Parameter(name = "Others")
    public boolean others = false;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoSwing").getEnabled();
    }
    public static NoSwing itz() {
        return ravex.manager.ModuleManager.delegate(NoSwing.class);
    }


}