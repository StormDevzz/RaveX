package ravex.modules.render;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.ColorParameter;
public class Fog extends Module {
    public final ColorParameter color = new ColorParameter("FogColor", 0xFFFF5500);

    public static boolean maybeEnabled() {
        return maybeEnabled(Fog.class);
    }

    public static Fog itz() {
        return ModuleManager.get(Fog.class);
    }
}
