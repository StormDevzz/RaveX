package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import java.util.List;
@ModuleInfo(name = "Weather", category = "Render")
public class Weather extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Rain", List.of("Clear", "Rain", "Snow", "Thunder"));

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Weather").getEnabled();
    }

    public static Weather itz() {
        return ravex.manager.ModuleManager.delegate(Weather.class);
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