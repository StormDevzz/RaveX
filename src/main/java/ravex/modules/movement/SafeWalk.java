package ravex.modules.movement;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "SafeWalk", category = "Movement")
public class SafeWalk {
    @Parameter(name = "Threshold", min = 0.0, max = 0.5, step = 0.001)
    public double threshold = 0.001;




}