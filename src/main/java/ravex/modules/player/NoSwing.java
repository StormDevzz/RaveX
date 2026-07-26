package ravex.modules.player;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
@ModuleInfo(name = "NoSwing", category = "Player")
public class NoSwing extends ravex.modules.Module {
public final BooleanParameter self = new BooleanParameter("Self", true);
    public final BooleanParameter others = new BooleanParameter("Others", false);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoSwing").getEnabled();
    }
    public static NoSwing itz() {
        return ravex.manager.ModuleManager.delegate(NoSwing.class);
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