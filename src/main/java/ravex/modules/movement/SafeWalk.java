package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "SafeWalk", category = "Movement")
public class SafeWalk implements ModuleAccess {
    @Parameter(name = "Threshold", min = 0.0, max = 0.5, step = 0.001)
    public double threshold = 0.001;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("SafeWalk").getEnabled();
    }
    public static SafeWalk itz() {
        return ravex.manager.ModuleManager.delegate(SafeWalk.class);
    }


}