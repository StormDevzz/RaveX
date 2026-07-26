package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
@ModuleInfo(name = "LiquidControl", category = "Movement")
public class LiquidControl extends ravex.modules.Module {
public final BooleanParameter water = new BooleanParameter("Water", true);
    public final BooleanParameter lava = new BooleanParameter("Lava", true);
    public final BooleanParameter others = new BooleanParameter("Others", true);
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("LiquidControl").getEnabled();
    }
    public static LiquidControl itz() {
        return ravex.manager.ModuleManager.delegate(LiquidControl.class);
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