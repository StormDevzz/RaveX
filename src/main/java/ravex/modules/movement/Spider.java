package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import java.util.List;
@ModuleInfo(name = "Spider", category = "Movement")
public class Spider implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Normal", "NCP", "Custom"})
    public String mode = "Normal";
    @Parameter(name = "Motion", min = 0.1, max = 0.6, step = 0.05)
    public double motion = 0.2;
    private Spider() {
        
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("Spider").getEnabled();
    }
    public static Spider itz() {
        return ravex.manager.ModuleManager.delegate(Spider.class);
    }


}