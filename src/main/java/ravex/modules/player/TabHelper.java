package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "TabHelper", category = "net.minecraft.world.entity.player.Player")
public class TabHelper implements ModuleAccess {
    @Parameter(name = "ShowPing")
    public boolean showPing = true;
    @Parameter(name = "MaxPlayers", min = 80.0, max = 1000.0, step = 10.0)
    public double limit = 250.0;
    @Parameter(name = "SelfColor", color = true)
    public int selfColor = 0xFF55FF55;
    @Parameter(name = "FriendColor", color = true)
    public int friendColor = 0xFFFF55FF;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("TabHelper").getEnabled();
    }
    public static TabHelper itz() {
        return ravex.manager.ModuleManager.delegate(TabHelper.class);
    }


}