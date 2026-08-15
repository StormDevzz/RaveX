package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "NoSwing", category = "Player")
public class NoSwing {
    @Parameter(name = "Self")
    public boolean self = true;
    @Parameter(name = "Others")
    public boolean others = false;




}