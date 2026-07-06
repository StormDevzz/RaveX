package ravex.modules.player;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.BooleanParameter;
public class MineAnimation extends Module {
    public static final MineAnimation INSTANCE = new MineAnimation();
    public final BooleanParameter hideSwing = new BooleanParameter("HideHandSwing", true);
    public final BooleanParameter hideCracks = new BooleanParameter("HideBlockCracks", true);

}
