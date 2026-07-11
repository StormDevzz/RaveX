package ravex.modules.render;
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
}
