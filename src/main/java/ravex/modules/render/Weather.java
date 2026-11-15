package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;
import ravex.modules.client.Notifications;
import ravex.utility.misc.LanguageUtility;
@Module(name = "Weather", category = "Render")
public class Weather {
    @Parameter(name = "Mode", modes = {"Clear", "Rain", "Snow", "Thunder"})
    public String mode = "Rain";

    public void onTick() {
        if (!Modules.enabled(NoRender.class)) return;
        NoRender noRender = Modules.get(NoRender.class);
        if (noRender == null || !noRender.weather) return;
        Notifications.notifyByMode(LanguageUtility.t("weather_norender_conflict"), 0xFFFF5555);
        Modules.setEnabled(Weather.class, false);
    }
}
