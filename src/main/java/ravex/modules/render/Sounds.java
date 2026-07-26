package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "Sounds", category = "Render")
public class Sounds extends ravex.modules.Module {
public final NumberParameter volume = new NumberParameter("Volume", 1.0, 0.0, 1.0, 0.1);
    private Sounds() {
        
        enabled = true;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Sounds").getEnabled();
    }

    public static Sounds itz() {
        return ravex.manager.ModuleManager.delegate(Sounds.class);
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