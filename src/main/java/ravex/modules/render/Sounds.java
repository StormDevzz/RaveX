package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "Sounds", category = "Render", enabled = true)
public class Sounds {
    @Parameter(name = "Volume", min = 0.0, max = 1.0, step = 0.1)
    public double volume = 1.0;
}