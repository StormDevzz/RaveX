package ravex.modules.player;

import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;

public class ExtraTab extends Module {
    public static final ExtraTab INSTANCE = new ExtraTab();

    public final BooleanParameter showPing = new BooleanParameter("Show Ping", true);
    public final NumberParameter limit = new NumberParameter("Max Players", 250.0, 80.0, 1000.0, 10.0);
    public final ColorParameter selfColor = new ColorParameter("Self Color", 0xFF55FF55);
    public final ColorParameter friendColor = new ColorParameter("Friend Color", 0xFFFF55FF);

    private ExtraTab() {
        super("ExtraTab", Category.PLAYER);
        addParameter(showPing);
        addParameter(limit);
        addParameter(selfColor);
        addParameter(friendColor);
    }
}
