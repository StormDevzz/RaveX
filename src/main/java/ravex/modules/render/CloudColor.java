package ravex.modules.render;
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
}
