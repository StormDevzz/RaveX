package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "TabHelper", category = "net.minecraft.world.entity.player.Player")
public class TabHelper extends ravex.modules.Module {
public final BooleanParameter showPing = new BooleanParameter("ShowPing", true);
    public final NumberParameter limit = new NumberParameter("MaxPlayers", 250.0, 80.0, 1000.0, 10.0);
    public final ColorParameter selfColor = new ColorParameter("SelfColor", 0xFF55FF55);
    public final ColorParameter friendColor = new ColorParameter("FriendColor", 0xFFFF55FF);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("TabHelper").getEnabled();
    }
    public static TabHelper itz() {
        return ravex.manager.ModuleManager.delegate(TabHelper.class);
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