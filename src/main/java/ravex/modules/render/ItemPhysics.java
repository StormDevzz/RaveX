package ravex.modules.render;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "ItemPhysics", category = "Render")
public class ItemPhysics implements ModuleAccess {
    @Parameter(name = "Scale", min = 0.1, max = 5.0, step = 0.1)
    public double scale = 1.0;

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("ItemPhysics").getEnabled();
    }

    public static ItemPhysics itz() {
        return ravex.manager.ModuleManager.delegate(ItemPhysics.class);
    }


}