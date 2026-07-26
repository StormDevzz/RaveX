package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "FastBreak", category = "net.minecraft.world.entity.player.Player")
public class FastBreak extends ravex.modules.Module {
public final NumberParameter delay = new NumberParameter("Delay", 0, 0, 4, 1);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("FastBreak").getEnabled();
    }
    public static FastBreak itz() {
        return ravex.manager.ModuleManager.delegate(FastBreak.class);
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