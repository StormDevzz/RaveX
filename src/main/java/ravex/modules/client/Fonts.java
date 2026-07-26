package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ToggleLockParameter;
import java.util.List;

@ModuleInfo(name = "Fonts", category = "Client")
public class Fonts implements ModuleAccess {
    @Parameter(name = "Enabled")
    public boolean p_enabled = true;
    @Parameter(name = "Font", modes = {"Comfortaa", "SFMedium", "SFBold", "Vanilla"})
    public String fontType = "SFBold";
    @Parameter(name = "FontSize", min = 0.5, max = 3.0, step = 0.1)
    public double fontSize = 1.0;
    @Parameter(name = "TextShadow")
    public boolean textShadow = true;
    @Parameter(name = "TextCase", modes = {"Normal", "Upper", "Lower"})
    public String textCase = "Normal";
    public final ToggleLockParameter lockToggle = new ToggleLockParameter("LockToggle", true);

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Fonts").getEnabled();
    }

    public static Fonts itz() {
        return ravex.manager.ModuleManager.delegate(Fonts.class);
    }

    public static String getActiveFont() {
        return ravex.manager.ModuleManager.delegate(Fonts.class).fontType;
    }

    public static float getActiveFontSize() {
        return (float) ravex.manager.ModuleManager.delegate(Fonts.class).fontSize;
    }

    public static boolean hasTextShadow() {
        return ravex.manager.ModuleManager.delegate(Fonts.class).textShadow;
    }

    public static String applyTextCase(String text) {
        String val = ravex.manager.ModuleManager.delegate(Fonts.class).textCase;
        if ("Upper".equals(val))
            return text.toUpperCase();
        if ("Lower".equals(val))
            return text.toLowerCase();
        return text;
    }


}