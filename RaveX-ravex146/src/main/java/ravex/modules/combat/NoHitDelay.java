package ravex.modules.combat;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;

public class NoHitDelay extends Module {
    public static final NoHitDelay INSTANCE = new NoHitDelay();

    public final BooleanParameter alwaysFull = new BooleanParameter("Always Full", true);

    private NoHitDelay() {
        super("NoHitDelay", Category.COMBAT);
        addParameter(alwaysFull);
    }
}
