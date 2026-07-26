package ravex.modules.combat;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "Hitboxes", category = "Combat")
public class Hitboxes implements ModuleAccess {
    @Parameter(name = "Size", min = 0.0, max = 2.0, step = 0.05)
    public double size = 0.3;

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Hitboxes").getEnabled();
    }
    public static Hitboxes itz() {
        return ravex.manager.ModuleManager.delegate(Hitboxes.class);
    }


}