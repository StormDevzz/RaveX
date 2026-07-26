package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "ItemPhysics", category = "Render")
public class ItemPhysics extends ravex.modules.Module {
public final NumberParameter scale = new NumberParameter("Scale", 1.0, 0.1, 5.0, 0.1);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ItemPhysics").getEnabled();
    }

    public static ItemPhysics itz() {
        return ravex.manager.ModuleManager.delegate(ItemPhysics.class);
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