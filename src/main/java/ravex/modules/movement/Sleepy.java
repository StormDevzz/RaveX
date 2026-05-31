package ravex.modules.movement;

import ravex.modules.Category;
import ravex.modules.Module;

public class Sleepy extends Module {
    public static final Sleepy INSTANCE = new Sleepy();

    private Sleepy() {
        super("Sleepy", Category.MOVEMENT);
    }
}
