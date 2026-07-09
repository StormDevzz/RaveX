package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.NumberParameter;

public class Sleepy extends Module {
    public static final Sleepy INSTANCE = new Sleepy();

    public final NumberParameter friction = new NumberParameter("Friction", 0.98, 0.6, 1.0, 0.01);
    public final BooleanParameter onlyOnGround = new BooleanParameter("Only On Ground", true);

    private Sleepy() {
        super("Sleepy", Category.MOVEMENT);
        addParameter(friction);
        addParameter(onlyOnGround);
    }
}

