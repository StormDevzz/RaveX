package ravex.modules.render;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class Ambient extends Module {
    public final NumberParameter r = new NumberParameter("Red", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter g = new NumberParameter("Green", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter b = new NumberParameter("Blue", 255.0, 0.0, 255.0, 1.0);
    public final NumberParameter a = new NumberParameter("Alpha", 30.0, 0.0, 255.0, 1.0);

    public static boolean maybeEnabled() {
        return maybeEnabled(Ambient.class);
    }

    public static Ambient itz() {
        return ModuleManager.get(Ambient.class);
    }
}
