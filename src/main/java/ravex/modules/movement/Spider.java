package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
@ModuleInfo(name = "Spider", category = "Movement")
public class Spider extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "NCP", "Custom"));
    public final NumberParameter motion = new NumberParameter("Motion", 0.2, 0.1, 0.6, 0.05);
    private Spider() {
        
        motion.setVisible(() -> "Custom".equals(mode.getValue()));
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Spider").getEnabled();
    }
    public static Spider itz() {
        return ravex.manager.ModuleManager.delegate(Spider.class);
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