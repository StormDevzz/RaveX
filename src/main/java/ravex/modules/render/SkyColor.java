package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
public class SkyColor extends Module {
    public final ColorParameter skyColor = new ColorParameter("SkyColor", 0xFF4FC3F7);

    public static boolean maybeEnabled() {
        return maybeEnabled(SkyColor.class);
    }

    public static SkyColor itz() {
        return ModuleManager.get(SkyColor.class);
    }
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
public class SkyColor extends Module {
    public static final SkyColor INSTANCE = new SkyColor();
    public final ColorParameter skyColor = new ColorParameter("SkyColor", 0xFF4FC3F7);

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
