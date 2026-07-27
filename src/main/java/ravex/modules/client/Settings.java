package ravex.modules.client;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ModeParameter;

@Module(name = "Settings", category = "Client", enabled = true)
public class Settings {
    @Parameter(name = "HeaderTextX", min = 10, max = 60, step = 1)
    public double headerTextX = 24;
    @Parameter(name = "ModuleTextX", min = 3, max = 30, step = 1)
    public double moduleTextX = 9;
    @Parameter(name = "MenuColor", color = true)
    public int menuColor = 0xFF0066FF;
    @Parameter(name = "Language", modes = {"English", "Russian"})
    public String ModeParameter = "English";
    private String prevLanguage = "English";
    public void onTick() {
        String lang = ModeParameter;
        if (!lang.equals(prevLanguage)) {
            prevLanguage = lang;
            ravex.utility.misc.LanguageUtility.setLanguage(lang);
        }
    }






}