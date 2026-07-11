package ravex.modules.player;
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
}
