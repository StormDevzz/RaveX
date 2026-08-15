package ravex.modules.player;
import ravex.modules.annotations.Module;
import ravex.modules.annotations.Parameter;
@Module(name = "TabHelper", category = "Player")
public class TabHelper {
    @Parameter(name = "ShowPing")
    public boolean showPing = true;
    @Parameter(name = "MaxPlayers", min = 80.0, max = 1000.0, step = 10.0)
    public double limit = 250.0;
    @Parameter(name = "SelfColor", color = true)
    public int selfColor = 0xFF55FF55;
    @Parameter(name = "FriendColor", color = true)
    public int friendColor = 0xFFFF55FF;




}