package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "FastBreak", category = "net.minecraft.world.entity.player.Player")
public class FastBreak {
    @Parameter(name = "Delay", min = 0, max = 4, step = 1)
    public double delay = 0;




}