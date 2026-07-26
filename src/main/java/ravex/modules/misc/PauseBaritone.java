package ravex.modules.misc;

import ravex.modules.annotations.ModuleInfo;
import ravex.integrations.baritone.BaritoneIntegration;

@ModuleInfo(name = "PauseBaritone", category = "Misc")
public class PauseBaritone extends ravex.modules.Module {
private final BaritoneIntegration baritone = new BaritoneIntegration();
    public void onEnable() {
        if (baritone.init()) {
            baritone.cancelPathing();
        }
        enabled = false;
    }

    public static PauseBaritone itz() {
        return ravex.manager.ModuleManager.delegate(PauseBaritone.class);
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