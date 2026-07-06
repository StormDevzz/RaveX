package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;

public class AutoTool extends Module {
    public static final AutoTool INSTANCE = new AutoTool();

    private final BooleanParameter silent = new BooleanParameter("Silent", true);

    private AutoTool() {
        super("AutoTool", Category.PLAYER);
        addParameter(silent);
    }
}
