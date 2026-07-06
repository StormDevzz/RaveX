package ravex.modules.client;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import ravex.parameter.Parameter;
import ravex.parameter.StringParameter;
public class Settings extends Module {
    public static final Settings INSTANCE = new Settings();
    public final NumberParameter headerTextX = new NumberParameter("HeaderTextX", 24, 10, 60, 1);
    public final NumberParameter moduleTextX = new NumberParameter("ModuleTextX", 9, 3, 30, 1);
    public final ravex.parameter.ColorParameter menuColor = new ravex.parameter.ColorParameter("MenuColor", 0xFF0066FF);
    public final BooleanParameter telemetry = new BooleanParameter("Telemetry", true);
    public final Parameter<String> language = new ModeParameter("Language", "English", java.util.List.of("English", "Russian"));
    private boolean prevTelemetry = false;
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
        boolean now = telemetry.getValue();
        if (now != prevTelemetry) {
            prevTelemetry = now;
            if (now) {
                ravex.manager.TelemetryManager.INSTANCE.sendTelemetry();
            } else {
                ravex.manager.TelemetryManager.INSTANCE.reset();
            }
        }
    }
}
