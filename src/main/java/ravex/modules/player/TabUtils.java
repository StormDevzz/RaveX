package ravex.modules.player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
import ravex.parameter.ColorParameter;
import ravex.parameter.NumberParameter;
public class TabUtils extends Module {
    public static final TabUtils INSTANCE = new TabUtils();
    public final BooleanParameter showPing = new BooleanParameter("ShowPing", true);
    public final NumberParameter limit = new NumberParameter("MaxPlayers", 250.0, 80.0, 1000.0, 10.0);
    public final ColorParameter selfColor = new ColorParameter("SelfColor", 0xFF55FF55);
    public final ColorParameter friendColor = new ColorParameter("FriendColor", 0xFFFF55FF);

}
