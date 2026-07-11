package ravex.modules.client;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.manager.ModuleManager;
import ravex.parameter.NumberParameter;
import ravex.parameter.Parameter;
public class Settings extends Module {
    public final NumberParameter headerTextX = new NumberParameter("HeaderTextX", 24, 10, 60, 1);
    public final NumberParameter moduleTextX = new NumberParameter("ModuleTextX", 9, 3, 30, 1);
    public final ravex.parameter.ColorParameter menuColor = new ravex.parameter.ColorParameter("MenuColor", 0xFF0066FF);
    public final Parameter<String> language = new ModeParameter("Language", "English", java.util.List.of("English", "Russian"));
    private String prevLanguage = "English";
    private Settings() {
        super("Settings");
        setEnabled(true);
    }
    @Override
    public void onTick() {
        String lang = language.getValue();
        if (!lang.equals(prevLanguage)) {
            prevLanguage = lang;
            ravex.utility.misc.LanguageUtility.setLanguage(lang);
        }
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(Settings.class);
    }

    public static Settings itz() {
        return ModuleManager.get(Settings.class);
    }
}
