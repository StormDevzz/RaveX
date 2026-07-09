package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class LiquidControl extends Module {
    public final BooleanParameter water = new BooleanParameter("Water", true);
    public final BooleanParameter lava = new BooleanParameter("Lava", true);
    public final BooleanParameter others = new BooleanParameter("Others", true);
    public static boolean maybeEnabled() {
        return maybeEnabled(LiquidControl.class);
    }
    public static LiquidControl itz() {
        return ModuleManager.get(LiquidControl.class);
    }
}
