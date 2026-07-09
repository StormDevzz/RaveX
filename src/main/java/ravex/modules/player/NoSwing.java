package ravex.modules.player;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class NoSwing extends Module {
    public final BooleanParameter self = new BooleanParameter("Self", true);
    public final BooleanParameter others = new BooleanParameter("Others", false);
    public static boolean maybeEnabled() {
        return maybeEnabled(NoSwing.class);
    }
    public static NoSwing itz() {
        return ModuleManager.get(NoSwing.class);
    }
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class NoSwing extends Module {
    public static final NoSwing INSTANCE = new NoSwing();
    public final BooleanParameter self = new BooleanParameter("Self", true);
    public final BooleanParameter others = new BooleanParameter("Others", false);

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
