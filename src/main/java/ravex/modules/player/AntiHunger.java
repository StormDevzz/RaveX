package ravex.modules.player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import java.util.List;
public class AntiHunger extends Module {
    public static final AntiHunger INSTANCE = new AntiHunger();
    public final ModeParameter mode = new ModeParameter("Mode", "Full", List.of("Full", "OnGround", "Sprint"));

}
