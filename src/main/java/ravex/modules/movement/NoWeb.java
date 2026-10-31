package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "NoWeb", category = "Movement")
public class NoWeb {
    @Parameter(name = "Mode", modes = {"Ignore", "Custom", "Grim"})
    public String mode = "Ignore";
    @Parameter(name = "Horizontal", min = 0.25, max = 1.0, step = 0.05, visible = "mode=Custom")
    public double horizontalSpeed = 0.8;
    @Parameter(name = "Vertical", min = 0.25, max = 1.0, step = 0.05, visible = "mode=Custom")
    public double verticalSpeed = 0.8;
    @Parameter(name = "ResetFall", visible = "mode=Custom")
    public boolean resetFall = true;
    // keep a little slow down for the grim prediction
    @Parameter(name = "GrimHorizontal", min = 0.3, max = 0.9, step = 0.05, visible = "mode=Grim")
    public double grimHorizontal = 0.5;
}
