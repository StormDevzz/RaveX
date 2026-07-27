package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
import ravex.modules.Modules;
@Module(name = "PingSpoof", category = "Misc")
public class PingSpoof {
    @Parameter(name = "Ping", min = 0, max = 50000, step = 100)
    public double ping = 1000;

    public int getSpoofedPing() {
        if (!Modules.enabled(PingSpoof.class)) return -1;
        return (int) ping;
    }






}