package ravex.modules.render;
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
}
