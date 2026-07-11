package ravex.modules.player;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class FastBreak extends Module {
    public final NumberParameter delay = new NumberParameter("Delay", 0, 0, 4, 1);
    public static boolean maybeEnabled() {
        return maybeEnabled(FastBreak.class);
    }
    public static FastBreak itz() {
        return ModuleManager.get(FastBreak.class);
    }
}
