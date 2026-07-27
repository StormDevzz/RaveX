package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import java.util.List;
@Module(name = "Spider", category = "Movement")
public class Spider {
    @Parameter(name = "Mode", modes = {"Normal", "NCP", "Custom"})
    public String mode = "Normal";
    @Parameter(name = "Motion", min = 0.1, max = 0.6, step = 0.05)
    public double motion = 0.2;
}