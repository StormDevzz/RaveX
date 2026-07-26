package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
@ModuleInfo(name = "PortalGui", category = "Misc")
public class PortalGui extends ravex.modules.Module {
public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("PortalGui").getEnabled();
    }

    public static PortalGui itz() {
        return ravex.manager.ModuleManager.delegate(PortalGui.class);
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