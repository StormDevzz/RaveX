package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class LiquidControl extends Module {
    public static final LiquidControl INSTANCE = new LiquidControl();
    public final BooleanParameter water = new BooleanParameter("Water", true);
    public final BooleanParameter lava = new BooleanParameter("Lava", true);
    public final BooleanParameter others = new BooleanParameter("Others", true);

}
