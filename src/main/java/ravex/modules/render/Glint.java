package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
@ModuleInfo(name = "Glint", category = "Render")
public class Glint extends ravex.modules.Module {
public final BooleanParameter items = new BooleanParameter("Items", true);
    public final BooleanParameter armor = new BooleanParameter("Armor", true);
    public final ColorParameter color = new ColorParameter("Color", 0xFFFF00FF);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Glint").getEnabled();
    }

    public static Glint itz() {
        return ravex.manager.ModuleManager.delegate(Glint.class);
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