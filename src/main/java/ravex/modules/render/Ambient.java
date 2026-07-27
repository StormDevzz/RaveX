package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "Ambient", category = "Render")
public class Ambient {
    @Parameter(name = "Red", min = 0.0, max = 255.0, step = 1.0)
    public double r = 255.0;
    @Parameter(name = "Green", min = 0.0, max = 255.0, step = 1.0)
    public double g = 255.0;
    @Parameter(name = "Blue", min = 0.0, max = 255.0, step = 1.0)
    public double b = 255.0;
    @Parameter(name = "Alpha", min = 0.0, max = 255.0, step = 1.0)
    public double a = 30.0;






}