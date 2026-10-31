package ravex.modules.client;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.parameter.ModeParameter;
import ravex.mcwrapper.MinecraftWrapper;

@Module(name = "Settings", category = "Client", enabled = true)
public class Settings {
    @Parameter(name = "ModuleTextX", min = 3, max = 30, step = 1)
    public double moduleTextX = 9;
    @Parameter(name = "MenuColor", color = true)
    public int menuColor = 0xFF0066FF;
    @Parameter(name = "Language", modes = {"English", "Russian"})
    public String ModeParameter = "English";
    private String prevLanguage = "English";
    private boolean languageModesSynced = false;

    public void onTick() {
        syncLanguageModes();
        String lang = ModeParameter;
        if (!lang.equals(prevLanguage)) {
            prevLanguage = lang;
            ravex.utility.misc.LanguageUtility.setLanguage(lang);
        }
    }

    // the dropdown lists every discovered language
    private void syncLanguageModes() {
        if (languageModesSynced) return;
        for (ravex.parameter.Parameter<?> p : ravex.manager.ModuleManager.delegate(Settings.class).getParameters()) {
            if (p instanceof ModeParameter mp && p.getName().equals("Language")) {
                mp.setModes(ravex.utility.misc.LanguageUtility.getLanguages());
                languageModesSynced = true;
                return;
            }
        }
        languageModesSynced = true;
    }




}
