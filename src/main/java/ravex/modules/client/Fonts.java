package ravex.modules.client;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.Modules;

@Module(name = "Fonts", category = "Client", enabled = true)
public class Fonts {
    @Parameter(name = "Font", modes = {"SFBold", "SFMedium", "Comfortaa", "Inter", "InterBold", "Vanilla"})
    public String fontType = "SFBold";
    @Parameter(name = "FontSize", min = 0.5, max = 3.0, step = 0.1)
    public double fontSize = 1.0;
    @Parameter(name = "TextShadow")
    public boolean textShadow = true;
    @Parameter(name = "TextCase", modes = {"Normal", "Upper", "Lower"})
    public String textCase = "Normal";
    @Parameter(name = "Chat")
    public boolean chatFont = false;
    // extra chat line spacing keeps taller fonts readable
    @Parameter(name = "ChatLineSpacing", min = 0.0, max = 1.0, step = 0.05, visible = "chatFont")
    public double chatLineSpacing = 0.25;

    private static boolean spacingApplied;

    // the font applies even when the module is off
    public static void applyChatLineSpacing() {
        var mc = MinecraftWrapper.getWrapper();
        if (mc.getRaw() == null || mc.getRaw().options == null) return;
        Fonts fonts = Modules.get(Fonts.class);
        if (fonts == null) return;
        var option = mc.getRaw().options.chatLineSpacing();
        if (Modules.enabled(Fonts.class) && fonts.chatFont) {
            if (Math.abs(option.get() - fonts.chatLineSpacing) > 0.001) {
                option.set(fonts.chatLineSpacing);
            }
            spacingApplied = true;
        } else if (spacingApplied) {
            if (option.get() != 0.0) option.set(0.0);
            spacingApplied = false;
        }
    }

    public void onTick() {
        applyChatLineSpacing();
    }





    // a disabled module leaves the vanilla look alone
    public static boolean isActive() {
        return Modules.enabled(Fonts.class);
    }

    public static String getActiveFont() {
        if (!isActive()) return "Vanilla";
        return Modules.get(Fonts.class).fontType;
    }

    public static float getActiveFontSize() {
        if (!isActive()) return 1.0f;
        return (float) Modules.get(Fonts.class).fontSize;
    }

    public static boolean hasTextShadow() {
        if (!isActive()) return true;
        return Modules.get(Fonts.class).textShadow;
    }

    public static String applyTextCase(String text) {
        if (!isActive()) return text;
        String val = Modules.get(Fonts.class).textCase;
        if ("Upper".equals(val))
            return text.toUpperCase();
        if ("Lower".equals(val))
            return text.toLowerCase();
        return text;
    }


}
