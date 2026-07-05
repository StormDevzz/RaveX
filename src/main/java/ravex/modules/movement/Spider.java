package ravex.modules.movement;
import ravex.modules.Category;
import ravex.modules.Module;
import ravex.parameter.ModeParameter;
import ravex.parameter.NumberParameter;
import java.util.List;
public class Spider extends Module {
    public static final Spider INSTANCE = new Spider();
    public final ModeParameter mode = new ModeParameter("Mode", "Normal", List.of("Normal", "NCP", "Custom"));
    public final NumberParameter motion = new NumberParameter("Motion", 0.2, 0.1, 0.6, 0.05);
    private Spider() {
        super("Spider");
        motion.setVisible(() -> "Custom".equals(mode.getValue()));
    }
}
