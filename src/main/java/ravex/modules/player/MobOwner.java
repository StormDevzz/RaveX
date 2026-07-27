package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.MobUtility;
import ravex.modules.Modules;
@Module(name = "MobOwner", category = "net.minecraft.world.entity.player.Player")
public class MobOwner {
    @Parameter(name = "Animals")
    public boolean animals = true;
    @Parameter(name = "ShowUUID")
    public boolean displayUUID = false;
    @Parameter(name = "Background")
    public boolean background = false;
    @Parameter(name = "TextColor", color = true)
    public int textColor = 0xFFFFAA00;

    public static String getOwnerName(net.minecraft.world.entity.LivingEntity entity) {
        return MobUtility.getOwnerName(entity, Modules.get(MobOwner.class).displayUUID);
    }




}