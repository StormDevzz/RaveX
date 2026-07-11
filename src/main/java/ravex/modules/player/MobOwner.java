package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.misc.MobUtility;
public class MobOwner extends Module {
    public final BooleanParameter animals = new BooleanParameter("Animals", true);
    public final BooleanParameter displayUUID = new BooleanParameter("ShowUUID", false);
    public final BooleanParameter background = new BooleanParameter("Background", false);
    public final ColorParameter textColor = new ColorParameter("TextColor", 0xFFFFAA00);

    public static String getOwnerName(net.minecraft.world.entity.LivingEntity entity) {
        return MobUtility.getOwnerName(entity, ModuleManager.get(MobOwner.class).displayUUID.getValue());
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(MobOwner.class);
    }
    public static MobOwner itz() {
        return ModuleManager.get(MobOwner.class);
    }

}