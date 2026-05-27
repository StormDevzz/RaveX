package ravex.modules.combat;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.NumberParameter;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ModeParameter;

public class KillAura extends Module {
    public static final KillAura INSTANCE = new KillAura();

    private final NumberParameter range = new NumberParameter("Range", 4.2, 3.0, 6.0, 0.1);
    private final BooleanParameter players = new BooleanParameter("Players", true);
    private final ModeParameter mode = new ModeParameter("Mode", "Single", java.util.List.of("Single", "Switch"));

    private KillAura() {
        super("KillAura", Category.COMBAT);
        addParameter(range);
        addParameter(players);
        addParameter(mode);
    }
}
