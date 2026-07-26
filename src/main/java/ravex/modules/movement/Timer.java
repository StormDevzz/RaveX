package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
@ModuleInfo(name = "Timer", category = "Movement")
public class Timer extends ravex.modules.Module {
public static float multiplier = 1.0f;
    public final NumberParameter speed = new NumberParameter("Speed", 2.0, 1.0, 20.0, 0.5);
    public final BooleanParameter strafeFix = new BooleanParameter("StrafeFix", true);
    public void onTick() {
        multiplier = speed.getValue().floatValue();
    }
    protected void onDisable() {
        multiplier = 1.0f;
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Timer").getEnabled();
    }
    public static Timer itz() {
        return ravex.manager.ModuleManager.delegate(Timer.class);
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