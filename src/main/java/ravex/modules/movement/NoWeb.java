package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class NoWeb extends Module {
    public static final NoWeb INSTANCE = new NoWeb();
    public final ModeParameter mode = new ModeParameter("Mode", "Positive",
            List.of("Custom", "Positive", "Positive2", "Positive3"));
    public final NumberParameter horizontalSpeed = new NumberParameter("Horizontal Speed", 1.0, 0.25, 1.0, 0.05);
    public final NumberParameter verticalSpeed   = new NumberParameter("Vertical Speed", 1.0, 0.05, 1.0, 0.05);
    private NoWeb() {
        super("NoWeb");
        horizontalSpeed.setVisible(() -> mode.getValue().equals("Custom"));
        verticalSpeed.setVisible(() -> mode.getValue().equals("Custom"));
    }
}
