package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.ToggleLockParameter;
import java.util.List;

public class Fonts extends Module {
    public static final Fonts INSTANCE = new Fonts();
    public final BooleanParameter enabled = new BooleanParameter("Enabled", true);
    public final ModeParameter fontType = new ModeParameter("Font", "SFBold",
            List.of("Comfortaa", "SFMedium", "SFBold", "Vanilla"));
    public final NumberParameter fontSize = new NumberParameter("FontSize", 1.0, 0.5, 3.0, 0.1);
    public final BooleanParameter textShadow = new BooleanParameter("TextShadow", true);
    public final ModeParameter textCase = new ModeParameter("TextCase", "Normal",
            List.of("Normal", "Upper", "Lower"));
    public final ToggleLockParameter lockToggle = new ToggleLockParameter("LockToggle", true);

    public static boolean isEnabled() {
        return INSTANCE.enabled.getValue();
    }

    public static String getActiveFont() {
        return INSTANCE.fontType.getValue();
    }

    public static float getActiveFontSize() {
        return INSTANCE.fontSize.getValue().floatValue();
    }

    public static boolean hasTextShadow() {
        return INSTANCE.textShadow.getValue();
    }

    public static String applyTextCase(String text) {
        String val = INSTANCE.textCase.getValue();
        if ("Upper".equals(val))
            return text.toUpperCase();
        if ("Lower".equals(val))
            return text.toLowerCase();
        return text;
    }
}
