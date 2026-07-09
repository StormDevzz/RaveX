package ravex.modules.movement;
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class Sleepy extends Module {
    public final NumberParameter friction = new NumberParameter("Friction", 0.98, 0.6, 1.0, 0.01);
    public final BooleanParameter onlyOnGround = new BooleanParameter("OnlyOnGround", true);
    public static boolean maybeEnabled() {
        return maybeEnabled(Sleepy.class);
    }
    public static Sleepy itz() {
        return ModuleManager.get(Sleepy.class);
    }
}
