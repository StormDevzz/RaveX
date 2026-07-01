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

    public final NumberParameter headerTextX = new NumberParameter("Header Text X", 24, 10, 60, 1);
    public final NumberParameter moduleTextX = new NumberParameter("Module Text X", 9, 3, 30, 1);
    public final ravex.parameter.ColorParameter menuColor = new ravex.parameter.ColorParameter("Menu Color", 0xFF0066FF);

    public final BooleanParameter telemetry = new BooleanParameter("Telemetry", true);

    public final BooleanParameter proxyEnabled = new BooleanParameter("Proxy", false);
    public final Parameter<String> proxyType = new ModeParameter("Proxy Type", "SOCKS5",
        java.util.List.of("SOCKS5", "SOCKS4", "HTTP"))
        .setVisible(() -> proxyEnabled.getValue());
    public final Parameter<String> proxyHost = new StringParameter("Proxy Host", "127.0.0.1")
        .setVisible(() -> proxyEnabled.getValue());
    public final Parameter<Double> proxyPort = new NumberParameter("Proxy Port", 1080.0, 1.0, 65535.0, 1.0)
        .setVisible(() -> proxyEnabled.getValue());
    public final Parameter<Boolean> proxyAuth = new BooleanParameter("Proxy Auth", false)
        .setVisible(() -> proxyEnabled.getValue());
    public final Parameter<String> proxyUsername = new StringParameter("Proxy Username", "")
        .setVisible(() -> proxyEnabled.getValue() && proxyAuth.getValue());
    public final Parameter<String> proxyPassword = new StringParameter("Proxy Password", "")
        .setVisible(() -> proxyEnabled.getValue() && proxyAuth.getValue());

    private boolean prevTelemetry = false;

    private Settings() {
        super("Settings", Category.CLIENT);
        addParameter(headerTextX);
        addParameter(moduleTextX);
        addParameter(menuColor);
        addParameter(telemetry);
        addParameter(proxyEnabled);
        addParameter(proxyType);
        addParameter(proxyHost);
        addParameter(proxyPort);
        addParameter(proxyAuth);
        addParameter(proxyUsername);
        addParameter(proxyPassword);
        setEnabled(true);
    }

    @Override
    public void onTick() {
        boolean now = telemetry.getValue();
        if (now != prevTelemetry) {
            prevTelemetry = now;
            if (now) {
                ravex.telemetry.TelemetryManager.INSTANCE.sendTelemetry();
            } else {
                ravex.telemetry.TelemetryManager.INSTANCE.reset();
            }
        }
    }
}
