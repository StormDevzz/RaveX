package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "Ambient", category = "Render")
public class Ambient extends ravex.modules.Module {
public final NumberParameter r = new NumberParameter("Red", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter g = new NumberParameter("Green", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter b = new NumberParameter("Blue", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter a = new NumberParameter("Alpha", 30.0, 0.0, 255.0, 1.0);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Ambient").getEnabled();
    }

    public static Ambient itz() {
        return ravex.manager.ModuleManager.delegate(Ambient.class);
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