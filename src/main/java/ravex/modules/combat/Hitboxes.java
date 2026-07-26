package ravex.modules.combat;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "Hitboxes", category = "Combat")
public class Hitboxes extends ravex.modules.Module {
public final NumberParameter size = new NumberParameter("Size", 0.3, 0.0, 2.0, 0.05);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Hitboxes").getEnabled();
    }
    public static Hitboxes itz() {
        return ravex.manager.ModuleManager.delegate(Hitboxes.class);
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