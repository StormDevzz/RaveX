package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
public class CloudColor extends Module {
    public final ColorParameter cloudColor = new ColorParameter("CloudColor", 0xFFFFFFFF);

    public static boolean maybeEnabled() {
        return maybeEnabled(CloudColor.class);
    }

    public static CloudColor itz() {
        return ModuleManager.get(CloudColor.class);
    }
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
public class CloudColor extends Module {
    public static final CloudColor INSTANCE = new CloudColor();
    public final ColorParameter cloudColor = new ColorParameter("CloudColor", 0xFFFFFFFF);

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
