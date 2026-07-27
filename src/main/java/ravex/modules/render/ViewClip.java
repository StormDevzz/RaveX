package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "ViewClip", category = "Render")
public class ViewClip {
    @Parameter(name = "BypassWalls")
    public boolean bypassWalls = true;
    @Parameter(name = "Distance", min = 1.0, max = 20.0, step = 0.5)
    public double cameraDistance = 4.0;






}