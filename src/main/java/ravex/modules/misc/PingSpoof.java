package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "PingSpoof", category = "Misc")
public class PingSpoof implements ModuleAccess {
    @Parameter(name = "Ping", min = 0, max = 50000, step = 100)
    public double ping = 1000;

    public int getSpoofedPing() {
        if (!ravex.manager.ModuleManager.INSTANCE.getByName("PingSpoof").getEnabled()) return -1;
        return (int) ping;
    }

    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("PingSpoof").getEnabled();
    }

    public static PingSpoof itz() {
        return ravex.manager.ModuleManager.delegate(PingSpoof.class);
    }


}