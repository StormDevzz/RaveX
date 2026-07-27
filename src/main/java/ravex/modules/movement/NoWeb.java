package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
@Module(name = "NoWeb", category = "Movement")
public class NoWeb {
    @Parameter(name = "Mode", modes = {"Vanilla", "Custom", "GrimStrict"})
    public String mode = "Vanilla";
    @Parameter(name = "HorizontalSpeed", min = 0.25, max = 1.0, step = 0.05)
    public double horizontalSpeed = 1.0;
    @Parameter(name = "VerticalSpeed", min = 0.05, max = 1.0, step = 0.05)
    public double verticalSpeed = 1.0;
}