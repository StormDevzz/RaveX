package ravex.modules.movement;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class NoWeb extends Module {
<<<<<<< HEAD
    public final ModeParameter mode = new ModeParameter("Mode", "Vanilla",
            List.of("Vanilla", "Custom", "GrimStrict"));
=======
    public static final NoWeb INSTANCE = new NoWeb();
    public final ModeParameter mode = new ModeParameter("Mode", "Positive",
            List.of("Custom", "Positive", "Positive2", "Positive3"));
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
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