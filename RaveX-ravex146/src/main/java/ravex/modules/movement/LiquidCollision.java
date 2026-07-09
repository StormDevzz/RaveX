package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;


public class LiquidCollision extends Module {
    public static final LiquidCollision INSTANCE = new LiquidCollision();

    public final BooleanParameter water = new BooleanParameter("Water", true);
    public final BooleanParameter lava = new BooleanParameter("Lava", true);
    public final BooleanParameter others = new BooleanParameter("Others", true);

    private LiquidCollision() {
        super("LiquidCollision", Category.MOVEMENT);
        addParameter(water);
        addParameter(lava);
        addParameter(others);
    }
}
