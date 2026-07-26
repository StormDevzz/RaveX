package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "Sleepy", category = "Movement")
public class Sleepy extends ravex.modules.Module {
public final NumberParameter friction = new NumberParameter("Friction", 0.98, 0.6, 1.0, 0.01);
    public final BooleanParameter onlyOnGround = new BooleanParameter("OnlyOnGround", true);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Sleepy").getEnabled();
    }
    public static Sleepy itz() {
        return ravex.manager.ModuleManager.delegate(Sleepy.class);
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