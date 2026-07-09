package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class SafeWalk extends Module {
    public final NumberParameter threshold = new NumberParameter("Threshold", 0.001, 0.0, 0.5, 0.001);
    public static boolean maybeEnabled() {
        return maybeEnabled(SafeWalk.class);
    }
    public static SafeWalk itz() {
        return ModuleManager.get(SafeWalk.class);
    }
}
