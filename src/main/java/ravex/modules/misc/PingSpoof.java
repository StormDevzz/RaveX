package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "PingSpoof", category = "Misc")
public class PingSpoof extends ravex.modules.Module {
public final NumberParameter ping = new NumberParameter("Ping", 1000, 0, 50000, 100);

    public int getSpoofedPing() {
        if (!getEnabled()) return -1;
        return ping.getValue().intValue();
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("PingSpoof").getEnabled();
    }

    public static PingSpoof itz() {
        return ravex.manager.ModuleManager.delegate(PingSpoof.class);
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