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
    private Settings() {
        super("Settings");
        setEnabled(true);
    }
    @Override
    public void onTick() {
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
