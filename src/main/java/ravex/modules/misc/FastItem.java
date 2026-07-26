package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "FastItem", category = "Misc")
public class FastItem implements ModuleAccess {
    @Parameter(name = "Delay", min = 0.0, max = 500.0, step = 10.0)
    public double delay = 0.0;

    public long getDelayMs() {
        return (long) delay;
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("FastItem").getEnabled();
    }

    public static FastItem itz() {
        return ravex.manager.ModuleManager.delegate(FastItem.class);
    }


}