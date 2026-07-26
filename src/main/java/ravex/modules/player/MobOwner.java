package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.utility.misc.MobUtility;
@ModuleInfo(name = "MobOwner", category = "Player")
public class MobOwner extends ravex.modules.Module {
public final BooleanParameter animals = new BooleanParameter("Animals", true);
    public final BooleanParameter displayUUID = new BooleanParameter("ShowUUID", false);
    public final BooleanParameter background = new BooleanParameter("Background", false);
    public final ColorParameter textColor = new ColorParameter("TextColor", 0xFFFFAA00);

    public static String getOwnerName(net.minecraft.world.entity.LivingEntity entity) {
        return MobUtility.getOwnerName(entity, ravex.manager.ModuleManager.delegate(MobOwner.class).displayUUID.getValue());
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("MobOwner").getEnabled();
    }
    public static MobOwner itz() {
        return ravex.manager.ModuleManager.delegate(MobOwner.class);
    }

    public java.util.List<ravex.parameter.Parameter<?>> getParameters() {
        java.util.List<ravex.parameter.Parameter<?>> list = new java.util.ArrayList<>();
        for (java.lang.reflect.Field field : getClass().getDeclaredFields()) {
            if (ravex.parameter.Parameter.class.isAssignableFrom(field.getType())) {
                try {
                    field.setAccessible(true);
                    list.add((ravex.parameter.Parameter<?>) field.get(this));
                } catch (Exception ignored) {}
            }
        }
        return list;
    }
}