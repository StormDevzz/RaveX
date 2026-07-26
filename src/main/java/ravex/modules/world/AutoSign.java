package ravex.modules.world;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.StringParameter;
@ModuleInfo(name = "AutoSign", category = "World")
public class AutoSign extends ravex.modules.Module {
public final StringParameter line1 = new StringParameter("Line1", "RaveX");
    public final StringParameter line2 = new StringParameter("Line2", "Client");
    public final StringParameter line3 = new StringParameter("Line3", "OnTop");
    public final StringParameter line4 = new StringParameter("Line4", "");

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AutoSign").getEnabled();
    }
    public static AutoSign itz() {
        return ravex.manager.ModuleManager.delegate(AutoSign.class);
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