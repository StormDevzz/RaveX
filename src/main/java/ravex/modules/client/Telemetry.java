package ravex.modules.client;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.telemetry.TelemetryManager;

public class Telemetry extends Module {
    public static final Telemetry INSTANCE = new Telemetry();

    private Telemetry() {
        super("Telemetry", Category.CLIENT);
    }

    @Override
    protected void onEnable() {
        TelemetryManager.INSTANCE.sendTelemetry();
    }

    @Override
    protected void onDisable() {
        TelemetryManager.INSTANCE.reset();
    }
}
