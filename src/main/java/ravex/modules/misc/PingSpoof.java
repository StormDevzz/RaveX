package ravex.modules.misc;
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.NumberParameter;
public class PingSpoof extends Module {
    public final NumberParameter ping = new NumberParameter("Ping", 1000, 0, 50000, 100);

    public int getSpoofedPing() {
        if (!getEnabled()) return -1;
        return ping.getValue().intValue();
    }

    public static boolean maybeEnabled() {
        return maybeEnabled(PingSpoof.class);
    }

    public static PingSpoof itz() {
        return ModuleManager.get(PingSpoof.class);
    }
}
