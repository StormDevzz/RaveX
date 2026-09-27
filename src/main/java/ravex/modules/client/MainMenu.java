package ravex.modules.client;

import java.io.File;
import java.util.Locale;
import ravex.manager.NotificationManager;
import ravex.mcwrapper.MinecraftWrapper;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.utility.misc.LanguageUtility;

@Module(name = "MainMenu", category = "Client", enabled = true)
public class MainMenu {
    @Parameter(name = "Mode", modes = {"Default", "Custom"})
    public String mode = "Default";
    @Parameter(name = "MenuBackground")
    public boolean menuBackground = true;
    @Parameter(name = "BgMode", modes = {"GIF", "Procedural"}, visible = "mode=Default")
    public String bgMode = "GIF";

    private String prevMode = "Default";

    public void onEnable() {
        getGifsFolder().mkdirs();
        prevMode = mode;
        if ("Custom".equals(mode)) checkGifs();
    }

    public void onTick() {
        if (!mode.equals(prevMode)) {
            prevMode = mode;
            if ("Custom".equals(mode)) checkGifs();
        }
    }

    private void checkGifs() {
        File folder = getGifsFolder();
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".gif"));
        if (files == null || files.length == 0) {
            NotificationManager.addToast(
                LanguageUtility.t("mainmenu_no_gifs", folder.getAbsolutePath()),
                0xFFFFAA33,
                NotificationManager.ToastType.INFO);
        }
    }

    public static File getGifsFolder() {
        File folder = new File(MinecraftWrapper.getWrapper().getRaw().gameDirectory, "RaveX/gifs");
        folder.mkdirs();
        return folder;
    }
}
