package ravex.modules.movement;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "LiquidControl", category = "Movement")
public class LiquidControl {
    @Parameter(name = "Water")
    public boolean water = true;
    @Parameter(name = "Lava")
    public boolean lava = true;
    @Parameter(name = "Others")
    public boolean others = true;




}