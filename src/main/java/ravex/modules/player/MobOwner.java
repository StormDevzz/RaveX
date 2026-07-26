package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.MobUtility;
@ModuleInfo(name = "MobOwner", category = "net.minecraft.world.entity.player.Player")
public class MobOwner implements ModuleAccess {
    @Parameter(name = "Animals")
    public boolean animals = true;
    @Parameter(name = "ShowUUID")
    public boolean displayUUID = false;
    @Parameter(name = "Background")
    public boolean background = false;
    @Parameter(name = "TextColor", color = true)
    public int textColor = 0xFFFFAA00;

    public static String getOwnerName(net.minecraft.world.entity.LivingEntity entity) {
        return MobUtility.getOwnerName(entity, ravex.manager.ModuleManager.delegate(MobOwner.class).displayUUID);
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MobOwner").getEnabled();
    }
    public static MobOwner itz() {
        return ravex.manager.ModuleManager.delegate(MobOwner.class);
    }


}