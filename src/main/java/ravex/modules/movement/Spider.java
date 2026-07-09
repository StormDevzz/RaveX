package ravex.modules.movement;
<<<<<<< HEAD
import ravex.manager.ModuleManager;
=======
import ravex.modules.Category;
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class Spider extends Module {
<<<<<<< HEAD
=======
    public static final Spider INSTANCE = new Spider();
>>>>>>> 1dd8ed59b0271ae3f636e53f56ee6c1c0c052ff3
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "NCP", "Custom"));
    public final NumberParameter motion = new NumberParameter("Motion", 0.2, 0.1, 0.6, 0.05);
    private Spider() {
        super("Spider");
        motion.setVisible(() -> "Custom".equals(mode.getValue()));
    }
    public static boolean maybeEnabled() {
        return maybeEnabled(Spider.class);
    }
    public static Spider itz() {
        return ModuleManager.get(Spider.class);
    }
}
