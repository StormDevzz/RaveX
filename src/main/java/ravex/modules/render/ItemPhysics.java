package ravex.modules.render;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "ItemPhysics", category = "Render")
public class ItemPhysics {
    @Parameter(name = "Scale", min = 0.1, max = 5.0, step = 0.1)
    public double scale = 1.0;






}