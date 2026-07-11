package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class NoWeb extends Module {
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
            List.of("Vanilla", "Custom", "GrimStrict"));
    public final NumberParameter horizontalSpeed = new NumberParameter("HorizontalSpeed", 1.0, 0.25, 1.0, 0.05);
    public final NumberParameter verticalSpeed   = new NumberParameter("VerticalSpeed", 1.0, 0.05, 1.0, 0.05);
    private NoWeb() {
        super("NoWeb");
        horizontalSpeed.setVisible(() -> mode.getValue().equals("Custom"));
        verticalSpeed.setVisible(() -> mode.getValue().equals("Custom"));
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(NoWeb.class);
    }
    public static NoWeb itz() {
        return ModuleManager.get(NoWeb.class);
    }
}