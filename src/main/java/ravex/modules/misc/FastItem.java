package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "FastItem", category = "Misc")
public class FastItem extends ravex.modules.Module {
public final NumberParameter delay = new NumberParameter("Delay", 0.0, 0.0, 500.0, 10.0);

    public long getDelayMs() {
        return delay.getValue().longValue();
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("FastItem").getEnabled();
    }

    public static FastItem itz() {
        return ravex.manager.ModuleManager.delegate(FastItem.class);
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