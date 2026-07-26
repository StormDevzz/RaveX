package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.ModuleInfo;
import ravex.modules.annotations.Parameter;
@ModuleInfo(name = "LiquidControl", category = "Movement")
public class LiquidControl implements ModuleAccess {
    @Parameter(name = "Water")
    public boolean water = true;
    @Parameter(name = "Lava")
    public boolean lava = true;
    @Parameter(name = "Others")
    public boolean others = true;
    public static boolean maybeEnabled() {
        return ravex.manager.ModuleManager.INSTANCE.getByName("LiquidControl").getEnabled();
    }
    public static LiquidControl itz() {
        return ravex.manager.ModuleManager.delegate(LiquidControl.class);
    }


}