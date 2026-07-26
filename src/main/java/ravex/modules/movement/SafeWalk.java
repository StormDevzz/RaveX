package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "SafeWalk", category = "Movement")
public class SafeWalk extends ravex.modules.Module {
public final NumberParameter threshold = new NumberParameter("Threshold", 0.001, 0.0, 0.5, 0.001);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("SafeWalk").getEnabled();
    }
    public static SafeWalk itz() {
        return ravex.manager.ModuleManager.delegate(SafeWalk.class);
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