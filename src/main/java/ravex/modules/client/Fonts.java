package ravex.modules.client;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ToggleLockParameter;
import ravex.modules.Modules;

@Module(name = "Fonts", category = "Client")
public class Fonts {
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





    public static String getActiveFont() {
        return Modules.get(Fonts.class).fontType;
    }

    public static float getActiveFontSize() {
        return (float) Modules.get(Fonts.class).fontSize;
    }

    public static boolean hasTextShadow() {
        return Modules.get(Fonts.class).textShadow;
    }

    public static String applyTextCase(String text) {
        String val = Modules.get(Fonts.class).textCase;
        if ("Upper".equals(val))
            return text.toUpperCase();
        if ("Lower".equals(val))
            return text.toLowerCase();
        return text;
    }


}