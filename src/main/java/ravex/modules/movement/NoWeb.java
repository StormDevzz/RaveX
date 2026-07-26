package ravex.modules.movement;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
@ModuleInfo(name = "NoWeb", category = "Movement")
public class NoWeb extends ravex.modules.Module {
public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
            List.of("Vanilla", "Custom", "GrimStrict"));
    public final NumberParameter horizontalSpeed = new NumberParameter("HorizontalSpeed", 1.0, 0.25, 1.0, 0.05);
    public final NumberParameter verticalSpeed   = new NumberParameter("VerticalSpeed", 1.0, 0.05, 1.0, 0.05);
    private NoWeb() {
        
        horizontalSpeed.setVisible(() -> mode.getValue().equals("Custom"));
        verticalSpeed.setVisible(() -> mode.getValue().equals("Custom"));
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoWeb").getEnabled();
    }
    public static NoWeb itz() {
        return ravex.manager.ModuleManager.delegate(NoWeb.class);
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