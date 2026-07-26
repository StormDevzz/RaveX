package ravex.modules.render;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
@ModuleInfo(name = "AspectRatio", category = "Render")
public class AspectRatio extends ravex.modules.Module {
public final ModeParameter ratio = new ModeParameter("Ratio", "16:9", List.of("16:9", "16:10", "4:3", "21:9", "Custom"));
    public final NumberParameter customWidth = new NumberParameter("Width", 16, 1, 100, 1);
    public final NumberParameter customHeight = new NumberParameter("Height", 9, 1, 100, 1);
    private AspectRatio() {
        
        customWidth.setVisible(() -> "Custom".equals(ratio.getValue()));
        customHeight.setVisible(() -> "Custom".equals(ratio.getValue()));
    }
    public float getAspectRatio(float original) {
        if (!getEnabled()) return original;
        return switch (ratio.getValue()) {
            case "16:9" -> 16f / 9f;
            case "16:10" -> 16f / 10f;
            case "4:3" -> 4f / 3f;
            case "21:9" -> 21f / 9f;
            case "Custom" -> customWidth.getValue().floatValue() / customHeight.getValue().floatValue();
            default -> original;
        };
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("AspectRatio").getEnabled();
    }

    public static AspectRatio itz() {
        return ravex.manager.ModuleManager.delegate(AspectRatio.class);
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