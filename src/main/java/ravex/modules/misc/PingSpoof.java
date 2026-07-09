package ravex.modules.misc;
<<<<<<< HEAD
import ravex.modules.Module;
import ravex.manager.ModuleManager;
import ravex.parameter.NumberParameter;
public class PingSpoof extends Module {
=======
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
public class PingSpoof extends Module {
    public static final PingSpoof INSTANCE = new PingSpoof();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final NumberParameter ping = new NumberParameter("Ping", 1000, 0, 50000, 100);

    public int getSpoofedPing() {
        if (!getEnabled()) return -1;
        return ping.getValue().intValue();
    }
<<<<<<< HEAD

    public static boolean maybeEnabled() {
        return maybeEnabled(PingSpoof.class);
    }

    public static PingSpoof itz() {
        return ModuleManager.get(PingSpoof.class);
    }
=======
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
}
