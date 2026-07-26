package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
import java.util.List;
@ModuleInfo(name = "NoWeb", category = "Movement")
public class NoWeb implements ModuleAccess {
    @Parameter(name = "Mode", modes = {"Vanilla", "Custom", "GrimStrict"})
    public String mode = "Vanilla";
    @Parameter(name = "HorizontalSpeed", min = 0.25, max = 1.0, step = 0.05)
    public double horizontalSpeed = 1.0;
    @Parameter(name = "VerticalSpeed", min = 0.05, max = 1.0, step = 0.05)
    public double verticalSpeed = 1.0;
    private NoWeb() {
        
    }
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("NoWeb").getEnabled();
    }
    public static NoWeb itz() {
        return ravex.manager.ModuleManager.delegate(NoWeb.class);
    }


}