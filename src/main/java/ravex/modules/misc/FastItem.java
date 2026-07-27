package ravex.modules.misc;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "FastItem", category = "Misc")
public class FastItem {
    @Parameter(name = "Delay", min = 0.0, max = 500.0, step = 10.0)
    public double delay = 0.0;

    public long getDelayMs() {
        return (long) delay;
    }






}