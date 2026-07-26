package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "Sleepy", category = "Movement")
public class Sleepy implements ModuleAccess {
    @Parameter(name = "Friction", min = 0.6, max = 1.0, step = 0.01)
    public double friction = 0.98;
    @Parameter(name = "OnlyOnGround")
    public boolean onlyOnGround = true;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Sleepy").getEnabled();
    }
    public static Sleepy itz() {
        return ravex.manager.ModuleManager.delegate(Sleepy.class);
    }


}