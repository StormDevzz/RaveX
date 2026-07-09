package ravex.modules.movement;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;
public class Sleepy extends Module {
<<<<<<< HEAD
    public final NumberParameter friction = new NumberParameter("Friction", 0.98, 0.6, 1.0, 0.01);
    public final BooleanParameter onlyOnGround = new BooleanParameter("OnlyOnGround", true);
    public static boolean maybeEnabled() {
        return maybeEnabled(Sleepy.class);
    }
    public static Sleepy itz() {
        return ModuleManager.get(Sleepy.class);
    }
=======
    public static final Sleepy INSTANCE = new Sleepy();
    public final NumberParameter friction = new NumberParameter("Friction", 0.98, 0.6, 1.0, 0.01);
    public final BooleanParameter onlyOnGround = new BooleanParameter("OnlyOnGround", true);

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
