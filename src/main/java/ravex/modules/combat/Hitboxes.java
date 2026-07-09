package ravex.modules.combat;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class Hitboxes extends Module {
    public final NumberParameter size = new NumberParameter("Size", 0.3, 0.0, 2.0, 0.05);

    public static boolean maybeEnabled() {
        return maybeEnabled(Hitboxes.class);
    }
    public static Hitboxes itz() {
        return ModuleManager.get(Hitboxes.class);
    }
}
