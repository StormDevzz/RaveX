package ravex.modules.render;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class ItemPhysics extends Module {
    public final NumberParameter scale = new NumberParameter("Scale", 1.0, 0.1, 5.0, 0.1);

    public static boolean maybeEnabled() {
        return maybeEnabled(ItemPhysics.class);
    }

    public static ItemPhysics itz() {
        return ModuleManager.get(ItemPhysics.class);
    }
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class ItemPhysics extends Module {
    public static final ItemPhysics INSTANCE = new ItemPhysics();
    public final NumberParameter scale = new NumberParameter("Scale", 1.0, 0.1, 5.0, 0.1);

>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
