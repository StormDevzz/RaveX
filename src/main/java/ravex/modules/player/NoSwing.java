package ravex.modules.player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class NoSwing extends Module {
    public static final NoSwing INSTANCE = new NoSwing();
    public final BooleanParameter self = new BooleanParameter("Self", true);
    public final BooleanParameter others = new BooleanParameter("Others", false);

}
