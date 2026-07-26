package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ModeParameter;

@ModuleInfo(name = "Settings", category = "Client")
public class Settings implements ModuleAccess {
    @Parameter(name = "HeaderTextX", min = 10, max = 60, step = 1)
    public double headerTextX = 24;
    @Parameter(name = "ModuleTextX", min = 3, max = 30, step = 1)
    public double moduleTextX = 9;
    @Parameter(name = "MenuColor", color = true)
    public int menuColor = 0xFF0066FF;
    @Parameter(name = "Language", modes = {"English", "Russian"})
    public String ModeParameter = "English";
    private String prevLanguage = "English";
    private Settings() {
        
        ravex.manager.ModuleManager.INSTANCE.getByName("Settings").setEnabled(true);
    }
    public void onTick() {
        String lang = ModeParameter;
        if (!lang.equals(prevLanguage)) {
            prevLanguage = lang;
            ravex.utility.misc.LanguageUtility.setLanguage(lang);
        }
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Settings").getEnabled();
    }

    public static Settings itz() {
        return ravex.manager.ModuleManager.delegate(Settings.class);
    }


}