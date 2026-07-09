package ravex.modules.misc;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.NumberParameter;
public class FastItem extends Module {
    public final NumberParameter delay = new NumberParameter("Delay", 0.0, 0.0, 500.0, 10.0);

    public long getDelayMs() {
        return delay.getValue().longValue();
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(FastItem.class);
    }

    public static FastItem itz() {
        return ModuleManager.get(FastItem.class);
    }
}
