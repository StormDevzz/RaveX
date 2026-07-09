package ravex.modules.render;
import ravex.manager.ModuleManager;

import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class ViewClip extends Module {
<<<<<<< HEAD
    public final BooleanParameter bypassWalls = new BooleanParameter("BypassWalls", true);
    public final NumberParameter cameraDistance = new NumberParameter("Distance", 4.0, 1.0, 20.0, 0.5);

    public static boolean maybeEnabled() {
        return maybeEnabled(ViewClip.class);
    }

    public static ViewClip itz() {
        return ModuleManager.get(ViewClip.class);
    }
=======
    public static final ViewClip INSTANCE = new ViewClip();
    public final BooleanParameter bypassWalls = new BooleanParameter("BypassWalls", true);
    public final NumberParameter cameraDistance = new NumberParameter("Distance", 4.0, 1.0, 20.0, 0.5);

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
