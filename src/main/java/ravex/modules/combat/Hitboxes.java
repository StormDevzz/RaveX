package ravex.modules.combat;
<<<<<<< HEAD
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
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class Hitboxes extends Module {
    public static final Hitboxes INSTANCE = new Hitboxes();
    public final NumberParameter size = new NumberParameter("Size", 0.3, 0.0, 2.0, 0.05);

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
