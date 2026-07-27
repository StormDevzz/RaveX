package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "Sleepy", category = "Movement")
public class Sleepy {
    @Parameter(name = "Friction", min = 0.6, max = 1.0, step = 0.01)
    public double friction = 0.98;
    @Parameter(name = "OnlyOnGround")
    public boolean onlyOnGround = true;




}