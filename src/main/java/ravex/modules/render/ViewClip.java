package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "ViewClip", category = "Render")
public class ViewClip implements ModuleAccess {
    @Parameter(name = "BypassWalls")
    public boolean bypassWalls = true;
    @Parameter(name = "Distance", min = 1.0, max = 20.0, step = 0.5)
    public double cameraDistance = 4.0;

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ViewClip").getEnabled();
    }

    public static ViewClip itz() {
        return ravex.manager.ModuleManager.delegate(ViewClip.class);
    }


}