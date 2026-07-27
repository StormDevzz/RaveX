package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "Hitboxes", category = "Combat")
public class Hitboxes {
    @Parameter(name = "Size", min = 0.0, max = 2.0, step = 0.05)
    public double size = 0.3;





}