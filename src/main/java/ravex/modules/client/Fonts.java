package ravex.modules.client;

import ravex.modules.annotations.ModuleInfo;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ToggleLockParameter;
import java.util.List;

@ModuleInfo(name = "Fonts", category = "Client")
public class Fonts extends ravex.modules.Module {
public final BooleanParameter p_enabled = new BooleanParameter("Enabled", true);
    public final ModeParameter fontType = new ModeParameter("Font", "SFBold",
            List.of("Comfortaa", "SFMedium", "SFBold", "Vanilla"));
    public final NumberParameter fontSize = new NumberParameter("FontSize", 1.0, 0.5, 3.0, 0.1);
    public final BooleanParameter textShadow = new BooleanParameter("TextShadow", true);
    public final ModeParameter textCase = new ModeParameter("TextCase", "Normal",
            List.of("Normal", "Upper", "Lower"));
    public final ToggleLockParameter lockToggle = new ToggleLockParameter("LockToggle", true);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Fonts").getEnabled();
    }

    public static Fonts itz() {
        return ravex.manager.ModuleManager.delegate(Fonts.class);
    }

    public static String getActiveFont() {
        return ravex.manager.ModuleManager.delegate(Fonts.class).fontType.getValue();
    }

    public static float getActiveFontSize() {
        return ravex.manager.ModuleManager.delegate(Fonts.class).fontSize.getValue().floatValue();
    }

    public static boolean hasTextShadow() {
        return ravex.manager.ModuleManager.delegate(Fonts.class).textShadow.getValue();
    }

    public static String applyTextCase(String text) {
        String val = ravex.manager.ModuleManager.delegate(Fonts.class).textCase.getValue();
        if ("Upper".equals(val))
            return text.toUpperCase();
        if ("Lower".equals(val))
            return text.toLowerCase();
        return text;
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