package ravex.modules.client;
import ravex.manager.ModuleManager;

import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ToggleLockParameter;
import java.util.List;

public class Fonts extends Module {
    public final BooleanParameter enabled = new BooleanParameter("Enabled", true);
    public final ModeParameter fontType = new ModeParameter("Font", "SFBold",
            List.of("Comfortaa", "SFMedium", "SFBold", "Vanilla"));
    public final NumberParameter fontSize = new NumberParameter("FontSize", 1.0, 0.5, 3.0, 0.1);
    public final BooleanParameter textShadow = new BooleanParameter("TextShadow", true);
    public final ModeParameter textCase = new ModeParameter("TextCase", "Normal",
            List.of("Normal", "Upper", "Lower"));
    public final ToggleLockParameter lockToggle = new ToggleLockParameter("LockToggle", true);

    public static boolean maybeEnabled() {
        return ModuleManager.get(Fonts.class).enabled.getValue();
    }

    public static Fonts itz() {
        return ModuleManager.get(Fonts.class);
    }

    public static String getActiveFont() {
        return ModuleManager.get(Fonts.class).fontType.getValue();
    }

    public static float getActiveFontSize() {
        return ModuleManager.get(Fonts.class).fontSize.getValue().floatValue();
    }

    public static boolean hasTextShadow() {
        return ModuleManager.get(Fonts.class).textShadow.getValue();
    }

    public static String applyTextCase(String text) {
        String val = ModuleManager.get(Fonts.class).textCase.getValue();
        if ("Upper".equals(val))
            return text.toUpperCase();
        if ("Lower".equals(val))
            return text.toLowerCase();
        return text;
    }
}
