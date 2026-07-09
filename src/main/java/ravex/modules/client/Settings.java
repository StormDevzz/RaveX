package ravex.modules.client;
<<<<<<< HEAD
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
=======
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
    public final BooleanParameter proxyEnabled = new BooleanParameter("Proxy", false);
    public final Parameter<String> proxyType = new ModeParameter("ProxyType", "SOCKS5",
        java.util.List.of("SOCKS5", "SOCKS4", "HTTP"))
        .setVisible(() -> proxyEnabled.getValue());
    public final Parameter<String> proxyHost = new StringParameter("ProxyHost", "127.0.0.1")
        .setVisible(() -> proxyEnabled.getValue());
    public final Parameter<Double> proxyPort = new NumberParameter("ProxyPort", 1080.0, 1.0, 65535.0, 1.0)
        .setVisible(() -> proxyEnabled.getValue());
    public final Parameter<Boolean> proxyAuth = new BooleanParameter("ProxyAuth", false)
        .setVisible(() -> proxyEnabled.getValue());
    public final Parameter<String> proxyUsername = new StringParameter("ProxyUsername", "")
        .setVisible(() -> proxyEnabled.getValue() && proxyAuth.getValue());
    public final Parameter<String> proxyPassword = new StringParameter("ProxyPassword", "")
        .setVisible(() -> proxyEnabled.getValue() && proxyAuth.getValue());
    private boolean prevTelemetry = false;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    private Settings() {
        super("Settings");
        setEnabled(true);
    }
    @Override
    public void onTick() {
<<<<<<< HEAD
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
=======
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
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
