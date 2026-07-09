package ravex.modules.player;
<<<<<<< HEAD
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
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class FastBreak extends Module {
    public static final FastBreak INSTANCE = new FastBreak();
    public final NumberParameter delay = new NumberParameter("Delay", 0, 0, 4, 1);

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
