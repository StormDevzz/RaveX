package ravex.modules.player;
import ravex.modules.ModuleAccess;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "NoSwing", category = "net.minecraft.world.entity.player.Player")
public class NoSwing {
    @Parameter(name = "Self")
    public boolean self = true;
    @Parameter(name = "Others")
    public boolean others = false;




}